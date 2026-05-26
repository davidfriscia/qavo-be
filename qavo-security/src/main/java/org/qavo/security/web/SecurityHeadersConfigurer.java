/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.web;

import org.qavo.security.config.QavoSecurityProperties;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

/**
 * Applies the platform's enforced secure HTTP response headers (architecture &sect;5.5): HSTS,
 * Content-Security-Policy, {@code X-Frame-Options: DENY}, {@code X-Content-Type-Options: nosniff},
 * Referrer-Policy, and Permissions-Policy. Values come from {@link QavoSecurityProperties.Headers}
 * so applications can tighten them; loosening requires an explicit override.
 *
 * <p>The Permissions-Policy header is written via a {@link StaticHeadersWriter} rather than the
 * version-specific DSL method, keeping the configuration stable across Spring Security minor versions.
 */
public final class SecurityHeadersConfigurer {

    private SecurityHeadersConfigurer() {
    }

    public static Customizer<HeadersConfigurer<HttpSecurity>> from(QavoSecurityProperties.Headers headers) {
        return configurer -> {
            configurer.contentTypeOptions(Customizer.withDefaults());
            configurer.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny);
            configurer.xssProtection(xss ->
                    xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK));
            configurer.referrerPolicy(referrer -> referrer.policy(
                    ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));

            if (headers.getPermissionsPolicy() != null && !headers.getPermissionsPolicy().isBlank()) {
                configurer.addHeaderWriter(
                        new StaticHeadersWriter("Permissions-Policy", headers.getPermissionsPolicy()));
            }

            if (headers.getContentSecurityPolicy() != null && !headers.getContentSecurityPolicy().isBlank()) {
                configurer.contentSecurityPolicy(csp -> csp.policyDirectives(headers.getContentSecurityPolicy()));
            }

            if (headers.isHstsEnabled()) {
                configurer.httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(headers.isHstsIncludeSubDomains())
                        .maxAgeInSeconds(headers.getHstsMaxAgeSeconds()));
            } else {
                configurer.httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable);
            }
        };
    }
}
