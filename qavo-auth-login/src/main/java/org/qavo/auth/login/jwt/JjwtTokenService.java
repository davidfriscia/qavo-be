/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;

import org.qavo.core.security.AuthenticatedPrincipal;
import org.qavo.security.config.QavoSecurityProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link TokenService} backed by JJWT for access-token signing and a Spring Data
 * repository for refresh-token storage. Refresh tokens are 256 bits of {@link SecureRandom}
 * entropy, Base64URL encoded on the wire and SHA-256 hashed in the database — a leak of the
 * table cannot be used to mint sessions. Refresh always rotates: the presented token is revoked
 * atomically and a fresh one is returned.
 *
 * <p>The class deliberately depends on {@link QavoSecurityProperties} (rather than its own
 * properties bag) so the signing material and policy live in a single place that the security
 * module's matching decoder also reads.
 */
public class JjwtTokenService implements TokenService {

    /** Length in bytes of the random refresh-token plaintext (256 bits). */
    public static final int REFRESH_TOKEN_BYTES = 32;

    /** Symmetric HMAC algorithm used for access-token signatures (RFC 7518 §3.2). */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final QavoSecurityProperties.Local.Jwt properties;
    private final RefreshTokenRepository refreshTokens;
    private final UserDetailsService userDetailsService;
    private final Clock clock;
    private final SecureRandom random;
    private final SecretKey signingKey;

    public JjwtTokenService(QavoSecurityProperties.Local.Jwt properties,
                            RefreshTokenRepository refreshTokens,
                            UserDetailsService userDetailsService,
                            Clock clock) {
        this.properties = properties;
        this.refreshTokens = refreshTokens;
        this.userDetailsService = userDetailsService;
        this.clock = clock;
        this.random = new SecureRandom();
        this.signingKey = buildSigningKey(properties.getSecret());
    }

    @Override
    @Transactional
    public IssuedTokens issueFor(AuthenticatedPrincipal principal) {
        Instant now = clock.instant();
        String accessToken = signAccessToken(principal, now);
        Instant accessExpiry = now.plus(properties.getAccessTokenDuration());

        String refreshPlaintext = randomRefreshToken();
        Instant refreshExpiry = now.plus(properties.getRefreshTokenDuration());
        refreshTokens.save(new RefreshToken(
                UUID.randomUUID(),
                sha256Hex(refreshPlaintext),
                principal.id(),
                now,
                refreshExpiry));

        return new IssuedTokens(accessToken, accessExpiry, refreshPlaintext, refreshExpiry);
    }

    @Override
    @Transactional
    public IssuedTokens refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RefreshTokenException();
        }
        String hash = sha256Hex(refreshToken);
        RefreshToken stored = refreshTokens.findByTokenHash(hash)
                .orElseThrow(RefreshTokenException::new);

        Instant now = clock.instant();
        if (!stored.isActive(now)) {
            throw new RefreshTokenException();
        }
        // Atomic rotation: revoke first, then mint. Both happen inside the same transaction.
        stored.revoke(now);
        refreshTokens.save(stored);

        // Reload the principal so the new access token reflects current authorities. If the user
        // has been disabled or removed since the original login, the lookup fails and the refresh
        // is rejected — refresh must not be a path around membership changes.
        AuthenticatedPrincipal principal;
        try {
            UserDetails details = userDetailsService.loadUserByUsername(stored.getUserId());
            if (!details.isEnabled() || !details.isAccountNonLocked()) {
                throw new RefreshTokenException();
            }
            principal = principalFor(details);
        } catch (UsernameNotFoundException ex) {
            throw new RefreshTokenException();
        }
        return issueFor(principal);
    }

    @Override
    @Transactional
    public void revokeAllFor(AuthenticatedPrincipal principal) {
        if (principal == null) {
            return;
        }
        refreshTokens.revokeAllActiveForUser(principal.id(), clock.instant());
    }

    /** Visible for testing — exposes the SHA-256 hex digest used as the lookup key. */
    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable on this JVM", ex);
        }
    }

    private String randomRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String signAccessToken(AuthenticatedPrincipal principal, Instant now) {
        Instant expiry = now.plus(properties.getAccessTokenDuration());
        JwtBuilder builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(principal.id())
                .issuer(properties.getIssuer())
                .audience().add(properties.getAudience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim(properties.getAuthoritiesClaim(), principal.roles());
        if (principal.username() != null) {
            builder.claim("preferred_username", principal.username());
        }
        return builder.signWith(signingKey, Jwts.SIG.HS256).compact();
    }

    private static SecretKey buildSigningKey(String base64Secret) {
        if (base64Secret == null || base64Secret.isBlank()) {
            throw new IllegalStateException(
                    "qavo.security.local.jwt.secret must be configured to issue tokens");
        }
        byte[] bytes = Base64.getDecoder().decode(base64Secret);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "qavo.security.local.jwt.secret must decode to at least 32 bytes (HMAC-SHA256)");
        }
        return new SecretKeySpec(bytes, HMAC_ALGORITHM);
    }

    /**
     * Builds an {@link AuthenticatedPrincipal} from a {@link UserDetails} loaded during refresh.
     * Mirrors the role/permission split used elsewhere in the platform: {@code ROLE_}-prefixed
     * authorities become roles (prefix stripped), the rest become permissions.
     */
    private AuthenticatedPrincipal principalFor(UserDetails details) {
        String name = details.getUsername();
        java.util.Set<String> roles = new java.util.LinkedHashSet<>();
        java.util.Set<String> permissions = new java.util.LinkedHashSet<>();
        for (GrantedAuthority authority : details.getAuthorities()) {
            String value = authority.getAuthority();
            if (value.startsWith("ROLE_")) {
                roles.add(value.substring(5));
            } else {
                permissions.add(value);
            }
        }
        return new ReloadedPrincipal(name, roles, permissions);
    }

    /** Snapshot principal projected from {@link UserDetails} during refresh. */
    private record ReloadedPrincipal(
            String id, java.util.Set<String> roles, java.util.Set<String> permissions)
            implements AuthenticatedPrincipal {
        @Override public String username() { return id; }
        @Override public java.util.Map<String, Object> attributes() { return java.util.Map.of(); }
    }
}
