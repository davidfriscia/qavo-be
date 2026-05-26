/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.config;

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
}
