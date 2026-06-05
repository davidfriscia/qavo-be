/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.qavo.security.AuthenticationStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Security configuration under {@code qavo.security.*} (see architecture &sect;5.5). Defaults are
 * "secure by default": strict headers are on, CORS is same-origin only, and the local strategy is
 * active out of the box. Applications loosen these only by explicit override.
 */
@ConfigurationProperties(prefix = "qavo.security")
public class QavoSecurityProperties {

    /** Active authentication strategy. */
    private AuthenticationStrategy strategy = AuthenticationStrategy.LOCAL;

    /** Ant-style paths that are always permitted without authentication. */
    private List<String> publicPaths = new ArrayList<>(List.of(
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"));

    @NestedConfigurationProperty
    private final Headers headers = new Headers();

    @NestedConfigurationProperty
    private final Cors cors = new Cors();

    @NestedConfigurationProperty
    private final Oidc oidc = new Oidc();

    @NestedConfigurationProperty
    private final Local local = new Local();

    public AuthenticationStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(AuthenticationStrategy strategy) {
        this.strategy = strategy;
    }

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }

    public Headers getHeaders() {
        return headers;
    }

    public Cors getCors() {
        return cors;
    }

    public Oidc getOidc() {
        return oidc;
    }

    public Local getLocal() {
        return local;
    }

    /** Secure HTTP response headers enforced by default (architecture &sect;5.5). */
    public static class Headers {

        /** Enable HSTS. Disable only for non-TLS local development. */
        private boolean hstsEnabled = true;

        /** HSTS max-age in seconds (default one year). */
        private long hstsMaxAgeSeconds = 31_536_000L;

        /** Apply HSTS to subdomains. */
        private boolean hstsIncludeSubDomains = true;

        /** Content-Security-Policy directive value. Restrictive default; tighten per application. */
        private String contentSecurityPolicy = "default-src 'self'; frame-ancestors 'none'; object-src 'none'";

        /** Referrer-Policy value. */
        private String referrerPolicy = "strict-origin-when-cross-origin";

        /** Permissions-Policy value disabling unnecessary browser features. */
        private String permissionsPolicy = "geolocation=(), microphone=(), camera=()";

        public boolean isHstsEnabled() {
            return hstsEnabled;
        }

        public void setHstsEnabled(boolean hstsEnabled) {
            this.hstsEnabled = hstsEnabled;
        }

        public long getHstsMaxAgeSeconds() {
            return hstsMaxAgeSeconds;
        }

        public void setHstsMaxAgeSeconds(long hstsMaxAgeSeconds) {
            this.hstsMaxAgeSeconds = hstsMaxAgeSeconds;
        }

        public boolean isHstsIncludeSubDomains() {
            return hstsIncludeSubDomains;
        }

        public void setHstsIncludeSubDomains(boolean hstsIncludeSubDomains) {
            this.hstsIncludeSubDomains = hstsIncludeSubDomains;
        }

        public String getContentSecurityPolicy() {
            return contentSecurityPolicy;
        }

        public void setContentSecurityPolicy(String contentSecurityPolicy) {
            this.contentSecurityPolicy = contentSecurityPolicy;
        }

        public String getReferrerPolicy() {
            return referrerPolicy;
        }

        public void setReferrerPolicy(String referrerPolicy) {
            this.referrerPolicy = referrerPolicy;
        }

        public String getPermissionsPolicy() {
            return permissionsPolicy;
        }

        public void setPermissionsPolicy(String permissionsPolicy) {
            this.permissionsPolicy = permissionsPolicy;
        }
    }

    /** Centralized CORS policy. Default is same-origin only (architecture &sect;5.6). */
    public static class Cors {

        /** Whether to register a CORS configuration at all. */
        private boolean enabled = false;

        /** Explicitly allowed origins for this deployment. Empty means same-origin only. */
        private List<String> allowedOrigins = new ArrayList<>();

        private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));

        private List<String> allowedHeaders = new ArrayList<>(List.of("*"));

        private boolean allowCredentials = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }
    }

    /** OIDC / OAuth2 resource-server configuration (architecture &sect;5.5). */
    public static class Oidc {

        /** Issuer URI of the OIDC provider; token validation is derived from it. */
        private String issuerUri;

        /** Optional explicit JWK set URI (otherwise discovered from the issuer). */
        private String jwkSetUri;

        /** JWT claim that carries roles/authorities (e.g. {@code roles}, {@code groups}). */
        private String authoritiesClaim = "roles";

        /** Prefix applied to mapped authorities. */
        private String authorityPrefix = "ROLE_";

        public String getIssuerUri() {
            return issuerUri;
        }

        public void setIssuerUri(String issuerUri) {
            this.issuerUri = issuerUri;
        }

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }

        public String getAuthoritiesClaim() {
            return authoritiesClaim;
        }

        public void setAuthoritiesClaim(String authoritiesClaim) {
            this.authoritiesClaim = authoritiesClaim;
        }

        public String getAuthorityPrefix() {
            return authorityPrefix;
        }

        public void setAuthorityPrefix(String authorityPrefix) {
            this.authorityPrefix = authorityPrefix;
        }
    }

    /**
     * Local authentication strategy configuration (architecture &sect;5.5). Groups every option
     * that is specific to the DB-backed authentication baseline so OIDC-only applications do not
     * see noise under {@code qavo.security.local.*} they cannot use.
     */
    public static class Local {

        @NestedConfigurationProperty
        private final Jwt jwt = new Jwt();

        @NestedConfigurationProperty
        private final Lockout lockout = new Lockout();

        public Jwt getJwt() {
            return jwt;
        }

        public Lockout getLockout() {
            return lockout;
        }

        /**
         * Locally issued JWT bearer token policy. The login plugin signs access tokens with
         * {@link #getSecret() secret} using HMAC-SHA256, embeds {@link #getIssuer() iss} and
         * {@link #getAudience() aud}, and the security module wires a matching local JWT decoder
         * so the same tokens are accepted on subsequent authenticated requests. The signing
         * material must be supplied by configuration; the platform never generates a key at
         * runtime to avoid silently rotating user sessions on restart.
         */
        public static class Jwt {

            /**
             * Base64-encoded HMAC-SHA256 signing key. Must decode to at least 32 bytes (256 bits).
             * Required when the local strategy is active and the login plugin is in use; the
             * platform validates this at startup via {@code EnvironmentPostProcessor} and fails
             * fast on missing/short keys.
             */
            private String secret;

            /** {@code iss} claim stamped on every issued token. */
            private String issuer = "qavo";

            /** {@code aud} claim stamped on every issued token. */
            private String audience = "qavo-clients";

            /** Validity window of issued access tokens. Default 30 minutes. */
            private Duration accessTokenDuration = Duration.ofMinutes(30);

            /** Validity window of issued refresh tokens. Default 7 days. */
            private Duration refreshTokenDuration = Duration.ofDays(7);

            /** JWT claim that carries roles. Aligned with the OIDC claim name for symmetry. */
            private String authoritiesClaim = "roles";

            /** Prefix applied to mapped authorities. */
            private String authorityPrefix = "ROLE_";

            public String getSecret() {
                return secret;
            }

            public void setSecret(String secret) {
                this.secret = secret;
            }

            public String getIssuer() {
                return issuer;
            }

            public void setIssuer(String issuer) {
                this.issuer = issuer;
            }

            public String getAudience() {
                return audience;
            }

            public void setAudience(String audience) {
                this.audience = audience;
            }

            public Duration getAccessTokenDuration() {
                return accessTokenDuration;
            }

            public void setAccessTokenDuration(Duration accessTokenDuration) {
                this.accessTokenDuration = accessTokenDuration;
            }

            public Duration getRefreshTokenDuration() {
                return refreshTokenDuration;
            }

            public void setRefreshTokenDuration(Duration refreshTokenDuration) {
                this.refreshTokenDuration = refreshTokenDuration;
            }

            public String getAuthoritiesClaim() {
                return authoritiesClaim;
            }

            public void setAuthoritiesClaim(String authoritiesClaim) {
                this.authoritiesClaim = authoritiesClaim;
            }

            public String getAuthorityPrefix() {
                return authorityPrefix;
            }

            public void setAuthorityPrefix(String authorityPrefix) {
                this.authorityPrefix = authorityPrefix;
            }
        }

        /**
         * Account lockout / brute-force protection policy. After {@link #getMaxAttempts()}
         * consecutive failed logins for the same username the account is locked for
         * {@link #getDuration()}. State is persisted on the user row so it survives restarts.
         */
        public static class Lockout {

            /** Whether the policy is active. */
            private boolean enabled = true;

            /** Consecutive failed-login threshold before lockout. */
            private int maxAttempts = 5;

            /** Lockout duration applied when the threshold is reached. */
            private Duration duration = Duration.ofMinutes(15);

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public int getMaxAttempts() {
                return maxAttempts;
            }

            public void setMaxAttempts(int maxAttempts) {
                this.maxAttempts = maxAttempts;
            }

            public Duration getDuration() {
                return duration;
            }

            public void setDuration(Duration duration) {
                this.duration = duration;
            }
        }
    }
}
