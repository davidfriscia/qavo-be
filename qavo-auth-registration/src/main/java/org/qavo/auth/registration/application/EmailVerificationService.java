/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.qavo.auth.registration.config.QavoRegistrationProperties;
import org.qavo.auth.registration.domain.QavoEmailVerificationToken;
import org.qavo.auth.registration.infrastructure.QavoEmailVerificationTokenRepository;
import org.qavo.core.notifications.NotificationDispatcher;
import org.qavo.core.notifications.NotificationRequest;
import org.qavo.security.local.domain.QavoUser;
import org.qavo.security.local.infrastructure.QavoUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the full email-verification lifecycle: issuing a fresh single-use token, dispatching the
 * verification email via the platform's {@link NotificationDispatcher}, and consuming the
 * token when the user follows the link. Token strength matches the refresh-token convention
 * (32 random bytes, URL-safe Base64) and only a SHA-256 hex digest is stored.
 *
 * <p>Notification dispatch is fail-soft on the issue path: a failed email send is logged at
 * {@code WARN} but does not roll back registration. Verification (consume) is strict: any
 * problem with the presented token raises one of the dedicated 4xx exceptions.
 */
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    /** Number of random bytes per raw token; matches the refresh-token convention. */
    private static final int TOKEN_BYTES = 32;

    private final QavoEmailVerificationTokenRepository tokenRepository;
    private final QavoUserRepository userRepository;
    private final NotificationDispatcher dispatcher;
    private final QavoRegistrationProperties properties;
    private final SecureRandom random;
    private final Clock clock;

    public EmailVerificationService(QavoEmailVerificationTokenRepository tokenRepository,
                                    QavoUserRepository userRepository,
                                    NotificationDispatcher dispatcher,
                                    QavoRegistrationProperties properties,
                                    Clock clock) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.dispatcher = dispatcher;
        this.properties = properties;
        this.clock = clock;
        this.random = new SecureRandom();
    }

    /**
     * Issue a fresh token for the user and (best-effort) deliver the verification email.
     * Returns the raw token primarily for tests; production callers should rely on the email
     * being delivered (or absent) and the {@code consumed} flag on the persisted row.
     */
    @Transactional
    public String issueFor(QavoUser user) {
        QavoRegistrationProperties.EmailVerification cfg = properties.getEmailVerification();
        String rawToken = randomToken();
        Instant now = Instant.now(clock);
        QavoEmailVerificationToken row = new QavoEmailVerificationToken(
                sha256Hex(rawToken),
                user.getId(),
                now.plus(cfg.getTokenDuration()),
                now);
        tokenRepository.save(row);

        if (dispatcher != null) {
            dispatch(user.getEmail(), cfg, rawToken);
        } else {
            log.warn("Email verification token issued for user={} but no NotificationDispatcher is configured",
                    user.getId());
        }
        return rawToken;
    }

    /**
     * Resolve the user by email and re-issue. Anti-enumeration: a missing email is logged and
     * returns silently so the controller can respond with HTTP 202 either way.
     */
    @Transactional
    public void resendFor(String email) {
        Optional<QavoUser> maybeUser = userRepository.findByEmail(email);
        if (maybeUser.isEmpty()) {
            log.info("Verification resend requested for unknown email (anti-enumeration: returning 202)");
            return;
        }
        QavoUser user = maybeUser.get();
        if (user.isEmailVerified()) {
            log.info("Verification resend requested for already-verified user={}", user.getId());
            return;
        }
        Instant since = Instant.now(clock).minus(Duration.ofHours(1));
        long recent = tokenRepository.countByUserIdAndCreatedAtGreaterThanEqual(user.getId(), since);
        int max = properties.getEmailVerification().getResendMaxPerHour();
        if (recent >= max) {
            // Approximate retry-after as the remainder of the rolling hour. Precise per-window
            // accounting would require tracking the oldest token timestamp; the spec calls only
            // for the count-per-hour ceiling, so this approximation is sufficient.
            throw new ResendRateLimitedException(Duration.ofHours(1).toSeconds());
        }
        issueFor(user);
    }

    /** Verify a presented raw token and flip the user's {@code emailVerified} flag. */
    @Transactional
    public void verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidVerificationTokenException();
        }
        QavoEmailVerificationToken row = tokenRepository.findById(sha256Hex(rawToken))
                .orElseThrow(InvalidVerificationTokenException::new);
        if (row.isConsumed()) {
            throw new VerificationTokenAlreadyUsedException();
        }
        if (!row.getExpiresAt().isAfter(Instant.now(clock))) {
            throw new VerificationTokenExpiredException();
        }
        QavoUser user = userRepository.findById(row.getUserId())
                .orElseThrow(InvalidVerificationTokenException::new);
        user.setEmailVerified(true);
        row.consume();
        userRepository.save(user);
        tokenRepository.save(row);
    }

    private void dispatch(String email, QavoRegistrationProperties.EmailVerification cfg, String rawToken) {
        String baseUrl = cfg.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            log.warn("qavo.auth.registration.email-verification.base-url is not configured; skipping send");
            return;
        }
        String link = "%s/api/v1/auth/verify-email?token=%s".formatted(stripTrailingSlash(baseUrl), rawToken);
        String body = "Welcome! Please verify your email by opening the link below:\n\n" + link
                + "\n\nIf you did not request this, ignore this message.";
        var result = dispatcher.dispatch(NotificationRequest.email(email, cfg.getSubject(), body));
        if (!result.success()) {
            log.warn("Verification email dispatch failed for {}: {}", email, result.errorMessage());
        }
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hex SHA-256, identical to the helper used by the login plugin's refresh tokens; copied
     * here to avoid a hard dependency from registration onto an internal helper in login.
     */
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

    /** Visible for tests so {@link UUID}-free composition stays readable. */
    Clock getClock() {
        return clock;
    }
}
