/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.autoconfigure;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.qavo.core.autoconfigure.QavoCoreAutoConfiguration;
import org.qavo.core.security.SecurityContextAccessor;
import org.qavo.security.config.QavoSecurityProperties;
import org.qavo.security.context.SpringSecurityContextAccessor;
import org.qavo.security.web.HttpSecurityCustomizer;
import org.qavo.security.web.PublicPathContributor;
import org.qavo.security.web.SecurityHeadersConfigurer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Core security auto-configuration. Establishes the secure-by-default baseline shared by every
 * strategy (architecture &sect;5.5): strict headers, centralized CORS, stateless sessions, method
 * security, and a uniform {@link SecurityContextAccessor}. Strategy-specific wiring (local DB,
 * OIDC) is layered on through {@link HttpSecurityCustomizer} beans contributed by sibling
 * configurations, so this class needs no knowledge of which strategy is active.
 *
 * <p>Ordered before the core auto-configuration so its Spring Security-backed
 * {@link SecurityContextAccessor} supersedes the core's anonymous fallback.
 */
@AutoConfiguration(before = QavoCoreAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(QavoSecurityProperties.class)
@EnableMethodSecurity
public class QavoSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SecurityContextAccessor securityContextAccessor() {
        return new SpringSecurityContextAccessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityFilterChain qavoSecurityFilterChain(
            HttpSecurity http,
            QavoSecurityProperties properties,
            @Nullable CorsConfigurationSource corsConfigurationSource,
            @Nullable List<HttpSecurityCustomizer> customizers,
            @Nullable List<PublicPathContributor> publicPathContributors) throws Exception {

        // Stateless, token-oriented REST API: CSRF protection is unnecessary and is disabled
        // deliberately. Authentication is carried per request (token or basic), never via a
        // server-side session cookie, so there is no CSRF surface to protect.
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.headers(SecurityHeadersConfigurer.from(properties.getHeaders()));

        if (corsConfigurationSource != null) {
            http.cors(cors -> cors.configurationSource(corsConfigurationSource));
        } else {
            http.cors(Customizer.withDefaults());
        }

        Set<String> publicPaths = new LinkedHashSet<>(properties.getPublicPaths());
        if (publicPathContributors != null) {
            for (PublicPathContributor contributor : publicPathContributors) {
                publicPaths.addAll(contributor.publicPaths());
            }
        }
        http.authorizeHttpRequests(registry -> registry
                .requestMatchers(publicPaths.toArray(new String[0])).permitAll()
                .anyRequest().authenticated());

        if (customizers != null) {
            for (HttpSecurityCustomizer customizer : customizers) {
                customizer.customize(http);
            }
        }

        return http.build();
    }

    /**
     * Registered only when {@code qavo.security.cors.enabled=true}; otherwise Spring Security's
     * restrictive same-origin default remains in effect (architecture &sect;5.6).
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "qavo.security.cors", name = "enabled", havingValue = "true")
    public CorsConfigurationSource qavoCorsConfigurationSource(QavoSecurityProperties properties) {
        QavoSecurityProperties.Cors cors = properties.getCors();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(cors.getAllowedOrigins());
        configuration.setAllowedMethods(cors.getAllowedMethods());
        configuration.setAllowedHeaders(cors.getAllowedHeaders());
        configuration.setAllowCredentials(cors.isAllowCredentials());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
