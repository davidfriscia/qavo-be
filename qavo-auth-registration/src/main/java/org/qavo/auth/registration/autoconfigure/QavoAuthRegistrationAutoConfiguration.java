/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.autoconfigure;

import java.util.List;

import org.qavo.auth.registration.api.RegistrationController;
import org.qavo.auth.registration.application.RegistrationService;
import org.qavo.auth.registration.config.QavoRegistrationProperties;
import org.qavo.core.api.ApiConventions;
import org.qavo.core.migration.MigrationLocation;
import org.qavo.core.plugin.PluginDescriptor;
import org.qavo.core.plugin.QavoPlugin;
import org.qavo.security.autoconfigure.QavoLocalAuthAutoConfiguration;
import org.qavo.security.local.infrastructure.QavoRoleRepository;
import org.qavo.security.local.infrastructure.QavoUserRepository;
import org.qavo.security.web.PublicPathContributor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Auto-configuration for the registration plugin (architecture &sect;6). Activates only when the
 * local auth baseline is present (its repositories are beans) and registration is enabled. Owns
 * its own Flyway migration location for verification tokens, contributes its public endpoint, and
 * publishes its plugin descriptor — all of which leave with the dependency if it is removed.
 */
@AutoConfiguration(after = QavoLocalAuthAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(QavoUserRepository.class)
@ConditionalOnExpression("'${qavo.security.strategy:local}'.toLowerCase() != 'oidc'")
@ConditionalOnProperty(prefix = "qavo.auth.registration", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(QavoRegistrationProperties.class)
public class QavoAuthRegistrationAutoConfiguration {

    public static final String PLUGIN_ID = "auth-registration";
    public static final String PLUGIN_VERSION = "0.0.1-SNAPSHOT";

    @Bean
    public RegistrationService qavoRegistrationService(QavoUserRepository userRepository,
                                                       QavoRoleRepository roleRepository,
                                                       PasswordEncoder passwordEncoder,
                                                       QavoRegistrationProperties properties) {
        return new RegistrationService(userRepository, roleRepository, passwordEncoder, properties);
    }

    @Bean
    public RegistrationController qavoRegistrationController(RegistrationService registrationService) {
        return new RegistrationController(registrationService);
    }

    @Bean
    public PublicPathContributor qavoRegistrationPublicPaths() {
        return () -> List.of(ApiConventions.AUTH_NAMESPACE + "/register");
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
