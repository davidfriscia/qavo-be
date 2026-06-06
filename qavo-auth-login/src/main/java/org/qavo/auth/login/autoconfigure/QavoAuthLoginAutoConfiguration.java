/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.autoconfigure;

import java.time.Clock;
import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.qavo.auth.login.api.LoginController;
import org.qavo.auth.login.config.QavoLoginProperties;
import org.qavo.auth.login.jwt.JjwtTokenService;
import org.qavo.auth.login.jwt.RefreshTokenRepository;
import org.qavo.auth.login.jwt.TokenService;
import org.qavo.core.api.ApiConventions;
import org.qavo.core.migration.MigrationLocation;
import org.qavo.core.plugin.PluginDescriptor;
import org.qavo.core.plugin.QavoPlugin;
import org.qavo.core.security.SecurityContextAccessor;
import org.qavo.security.config.QavoSecurityProperties;
import org.qavo.security.local.lockout.LockoutService;
import org.qavo.security.web.PublicPathContributor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Auto-configuration for the login plugin (architecture &sect;6). Activates when the plugin is on
 * the classpath, the application is a servlet web app, and {@code qavo.auth.login.enabled} is not
 * disabled. It registers the login controller, the {@link TokenService} bean that signs and
 * rotates bearer tokens, the public paths of the login and refresh endpoints, and contributes the
 * plugin's Flyway migration location plus the JPA entity/repository scan augmentations.
 */
@AutoConfiguration(before = HibernateJpaAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({AuthenticationManager.class, EntityManagerFactory.class})
@ConditionalOnProperty(prefix = "qavo.auth.login", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(QavoLoginProperties.class)
@Import(QavoAuthLoginPackageRegistrar.class)
@EnableTransactionManagement
public class QavoAuthLoginAutoConfiguration {

    public static final String PLUGIN_ID = "auth-login";
    public static final String PLUGIN_VERSION = "0.0.3-SNAPSHOT";

    @Bean
    @ConditionalOnMissingBean
    public LoginController qavoLoginController(AuthenticationManager authenticationManager,
                                               SecurityContextAccessor securityContextAccessor,
                                               TokenService tokenService,
                                               LockoutService lockoutService,
                                               org.qavo.security.local.infrastructure.QavoUserRepository userRepository,
                                               @org.springframework.beans.factory.annotation.Value(
                                                       "${qavo.auth.registration.email-verification.require-verified-email-to-login:false}")
                                               boolean requireVerifiedEmail) {
        return new LoginController(authenticationManager, securityContextAccessor, tokenService,
                lockoutService, userRepository, requireVerifiedEmail);
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenService qavoTokenService(QavoSecurityProperties securityProperties,
                                         RefreshTokenRepository refreshTokenRepository,
                                         UserDetailsService userDetailsService,
                                         Clock clock) {
        return new JjwtTokenService(
                securityProperties.getLocal().getJwt(),
                refreshTokenRepository,
                userDetailsService,
                clock);
    }

    /** Default monotonic UTC clock; tests substitute a fixed clock to simulate time passage. */
    @Bean
    @ConditionalOnMissingBean
    public Clock qavoLoginClock() {
        return Clock.systemUTC();
    }

    @Bean
    public PublicPathContributor qavoLoginPublicPaths() {
        // Login and refresh must be reachable without a bearer token by definition. Logout
        // requires an authenticated principal so it is intentionally NOT listed here.
        return () -> List.of(
                ApiConventions.AUTH_NAMESPACE + "/login",
                ApiConventions.AUTH_NAMESPACE + "/refresh");
    }

    @Bean
    public MigrationLocation qavoAuthLoginMigrationLocation() {
        return MigrationLocation.of("classpath:db/qavo/auth-login", "qavo-auth-login");
    }

    @Bean
    public QavoPlugin qavoAuthLoginPlugin() {
        return new PluginDescriptor(PLUGIN_ID, "Local Login", PLUGIN_VERSION,
                "Local credential login with JWT bearer tokens under " + ApiConventions.AUTH_NAMESPACE);
    }
}
