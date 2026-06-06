/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.autoconfigure;

import java.time.Clock;
import java.util.List;

import org.qavo.auth.registration.api.EmailVerificationController;
import org.qavo.auth.registration.api.RegistrationController;
import org.qavo.auth.registration.api.RegistrationStatusController;
import org.qavo.auth.registration.application.DatabaseRegistrationCapService;
import org.qavo.auth.registration.application.EmailVerificationService;
import org.qavo.auth.registration.application.NoOpRegistrationCapService;
import org.qavo.auth.registration.application.RegistrationService;
import org.qavo.auth.registration.config.QavoRegistrationCapProperties;
import org.qavo.auth.registration.config.QavoRegistrationProperties;
import org.qavo.auth.registration.infrastructure.QavoEmailVerificationTokenRepository;
import org.qavo.auth.registration.infrastructure.RegistrationEventRepository;
import org.qavo.core.api.ApiConventions;
import org.qavo.core.migration.MigrationLocation;
import org.qavo.core.notifications.NotificationDispatcher;
import org.qavo.core.plugin.PluginDescriptor;
import org.qavo.core.plugin.QavoPlugin;
import org.qavo.core.registration.RegistrationCapService;
import org.qavo.security.autoconfigure.QavoLocalAuthAutoConfiguration;
import org.qavo.security.local.infrastructure.QavoRoleRepository;
import org.qavo.security.local.infrastructure.QavoUserRepository;
import org.qavo.security.web.PublicPathContributor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Auto-configuration for the registration plugin (architecture &sect;6). Activates only when the
 * local auth baseline is present (its repositories are beans) and registration is enabled. Owns
 * its own Flyway migration location for verification tokens, contributes its public endpoints,
 * and publishes its plugin descriptor — all of which leave with the dependency if it is removed.
 *
 * <p>Since 0.0.2-SNAPSHOT this configuration also wires the email-verification flow: the
 * service that issues + consumes tokens, the controller that exposes the verify/resend
 * endpoints, and an extra public-path contribution. The {@link NotificationDispatcher} is
 * injected via {@link ObjectProvider} so the plugin keeps loading even when notifications are
 * disabled — verification simply degrades to "issue a token but do not send" with a WARN log.
 *
 * <p>Since 0.0.3-SNAPSHOT it also wires the registration cap (ADR 0012): either the
 * database-backed {@link DatabaseRegistrationCapService} (when
 * {@code qavo.auth.registration.cap.enabled=true}) or the {@link NoOpRegistrationCapService}
 * (the default). The {@link RegistrationStatusController} exposes the public read-only
 * cap-status endpoint regardless of which implementation is active.
 */
@AutoConfiguration(after = QavoLocalAuthAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(QavoUserRepository.class)
@ConditionalOnExpression("'${qavo.security.strategy:local}'.toLowerCase() != 'oidc'")
@ConditionalOnProperty(prefix = "qavo.auth.registration", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties({QavoRegistrationProperties.class, QavoRegistrationCapProperties.class})
@Import(QavoAuthRegistrationPackageRegistrar.class)
public class QavoAuthRegistrationAutoConfiguration {

    public static final String PLUGIN_ID = "auth-registration";
    public static final String PLUGIN_VERSION = "0.0.3-SNAPSHOT";

    @Bean
    @ConditionalOnMissingBean
    public EmailVerificationService qavoEmailVerificationService(
            QavoEmailVerificationTokenRepository tokenRepository,
            QavoUserRepository userRepository,
            ObjectProvider<NotificationDispatcher> dispatcherProvider,
            QavoRegistrationProperties properties,
            Clock clock) {
        return new EmailVerificationService(
                tokenRepository,
                userRepository,
                dispatcherProvider.getIfAvailable(),
                properties,
                clock);
    }

    /**
     * Fallback {@link MeterRegistry} so the cap services remain functional in deployments that
     * do not pull in Micrometer's auto-configuration. {@code @ConditionalOnMissingBean} ensures
     * the real registry (e.g. Prometheus) wins when present.
     */
    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    public MeterRegistry qavoRegistrationFallbackMeterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(RegistrationCapService.class)
    @ConditionalOnProperty(prefix = "qavo.auth.registration.cap", name = "enabled",
            havingValue = "true")
    public RegistrationCapService qavoDatabaseRegistrationCapService(
            RegistrationEventRepository events,
            QavoUserRepository users,
            QavoRegistrationCapProperties capProperties,
            Clock clock,
            MeterRegistry meterRegistry) {
        return new DatabaseRegistrationCapService(events, users, capProperties, clock, meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(RegistrationCapService.class)
    @ConditionalOnProperty(prefix = "qavo.auth.registration.cap", name = "enabled",
            havingValue = "false", matchIfMissing = true)
    public RegistrationCapService qavoNoOpRegistrationCapService(Clock clock,
                                                                 MeterRegistry meterRegistry) {
        return new NoOpRegistrationCapService(clock, meterRegistry);
    }

    @Bean
    public RegistrationService qavoRegistrationService(QavoUserRepository userRepository,
                                                       QavoRoleRepository roleRepository,
                                                       PasswordEncoder passwordEncoder,
                                                       QavoRegistrationProperties properties,
                                                       EmailVerificationService emailVerificationService,
                                                       RegistrationCapService capService) {
        return new RegistrationService(userRepository, roleRepository, passwordEncoder, properties,
                emailVerificationService, capService);
    }

    @Bean
    public RegistrationController qavoRegistrationController(RegistrationService registrationService) {
        return new RegistrationController(registrationService);
    }

    @Bean
    public EmailVerificationController qavoEmailVerificationController(
            EmailVerificationService verificationService) {
        return new EmailVerificationController(verificationService);
    }

    @Bean
    public RegistrationStatusController qavoRegistrationStatusController(
            RegistrationCapService capService) {
        return new RegistrationStatusController(capService);
    }

    @Bean
    public PublicPathContributor qavoRegistrationPublicPaths() {
        return () -> List.of(
                ApiConventions.AUTH_NAMESPACE + "/register",
                ApiConventions.AUTH_NAMESPACE + "/registration-status",
                ApiConventions.AUTH_NAMESPACE + "/verify-email",
                ApiConventions.AUTH_NAMESPACE + "/verify-email/resend");
    }

    @Bean
    public MigrationLocation qavoRegistrationMigrationLocation() {
        return MigrationLocation.of("classpath:db/qavo/auth-registration", "qavo-auth-registration");
    }

    @Bean
    public QavoPlugin qavoAuthRegistrationPlugin() {
        return new PluginDescriptor(PLUGIN_ID, "Self-Service Registration", PLUGIN_VERSION,
                "User registration under " + ApiConventions.AUTH_NAMESPACE + "/register");
    }
}
