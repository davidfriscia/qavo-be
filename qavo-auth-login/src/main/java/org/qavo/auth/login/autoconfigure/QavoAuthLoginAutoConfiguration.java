/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.autoconfigure;

import java.util.List;

import org.qavo.auth.login.api.LoginController;
import org.qavo.auth.login.config.QavoLoginProperties;
import org.qavo.core.api.ApiConventions;
import org.qavo.core.plugin.PluginDescriptor;
import org.qavo.core.plugin.QavoPlugin;
import org.qavo.core.security.SecurityContextAccessor;
import org.qavo.security.web.PublicPathContributor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;

/**
 * Auto-configuration for the login plugin (architecture &sect;6). Activates when the plugin is on
 * the classpath, the application is a servlet web app, and {@code qavo.auth.login.enabled} is not
 * disabled. It registers the login controller, declares the login endpoint as public, and
 * publishes the plugin descriptor so the platform's plugin inventory reflects its presence.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(AuthenticationManager.class)
@ConditionalOnProperty(prefix = "qavo.auth.login", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(QavoLoginProperties.class)
public class QavoAuthLoginAutoConfiguration {

    public static final String PLUGIN_ID = "auth-login";
    public static final String PLUGIN_VERSION = "0.0.0-SNAPSHOT";

    @Bean
    public LoginController qavoLoginController(AuthenticationManager authenticationManager,
                                               SecurityContextAccessor securityContextAccessor) {
        return new LoginController(authenticationManager, securityContextAccessor);
    }

    @Bean
    public PublicPathContributor qavoLoginPublicPaths() {
        return () -> List.of(ApiConventions.AUTH_NAMESPACE + "/login");
    }

    @Bean
    public QavoPlugin qavoAuthLoginPlugin() {
        return new PluginDescriptor(PLUGIN_ID, "Local Login", PLUGIN_VERSION,
                "Local credential login flow under " + ApiConventions.AUTH_NAMESPACE + "/login");
    }
}
