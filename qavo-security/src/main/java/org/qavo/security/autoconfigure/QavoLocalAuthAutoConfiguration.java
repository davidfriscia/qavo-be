/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.autoconfigure;

import jakarta.persistence.EntityManagerFactory;

import java.time.Clock;

import org.qavo.core.migration.MigrationLocation;
import org.qavo.security.config.QavoSecurityProperties;
import org.qavo.security.local.application.QavoUserDetailsService;
import org.qavo.security.local.infrastructure.QavoUserRepository;
import org.qavo.security.local.lockout.AuthenticationEventListener;
import org.qavo.security.local.lockout.LockoutService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

/**
 * Local DB authentication baseline (architecture &sect;5.5). Activates when JPA is on the
 * classpath and the strategy is {@code local} or {@code hybrid} (i.e. anything but pure
 * {@code oidc}). It registers the platform's user/role entities and repositories, exposes a
 * {@link QavoUserDetailsService} (from which Spring Boot derives the {@code AuthenticationManager}),
 * and contributes the security module's Flyway migrations so the tables are created automatically.
 *
 * <p>An OIDC-only application excludes JPA and never triggers this configuration. Entity and
 * repository discovery is handled by {@link QavoLocalAuthPackageRegistrar}, which augments the
 * application's scanned packages rather than replacing them.
 */
@AutoConfiguration(before = HibernateJpaAutoConfiguration.class)
@ConditionalOnClass({EntityManagerFactory.class, JpaRepository.class})
@ConditionalOnExpression("'${qavo.security.strategy:local}'.toLowerCase() != 'oidc'")
@Import(QavoLocalAuthPackageRegistrar.class)
public class QavoLocalAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public QavoUserDetailsService qavoUserDetailsService(QavoUserRepository userRepository,
                                                         LockoutService lockoutService) {
        return new QavoUserDetailsService(userRepository, lockoutService);
    }

    @Bean
    @ConditionalOnMissingBean
    public LockoutService qavoLockoutService(QavoUserRepository userRepository,
                                             QavoSecurityProperties securityProperties,
                                             Clock clock) {
        return new LockoutService(userRepository,
                securityProperties.getLocal().getLockout(),
                clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationEventListener qavoAuthenticationEventListener(LockoutService lockoutService) {
        return new AuthenticationEventListener(lockoutService);
    }

    /**
     * Default UTC clock shared by the local-auth components. A test or production application
     * may publish its own {@code Clock} bean to make time deterministic or zone-specific.
     */
    @Bean
    @ConditionalOnMissingBean
    public Clock qavoSecurityClock() {
        return Clock.systemUTC();
    }

    /**
     * Exposes the {@link AuthenticationManager} so credential-based plugins (notably the login
     * plugin) can authenticate against the local store. Backed by the {@code DaoAuthenticationProvider}
     * that Spring derives from the {@link QavoUserDetailsService} and {@code PasswordEncoder} beans.
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public MigrationLocation qavoSecurityMigrationLocation() {
        return MigrationLocation.of("classpath:db/qavo/security", "qavo-security");
    }
}
