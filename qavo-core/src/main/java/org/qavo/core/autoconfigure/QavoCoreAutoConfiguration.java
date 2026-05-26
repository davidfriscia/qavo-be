/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.autoconfigure;

import java.net.URI;
import java.time.Clock;
import java.util.List;

import org.qavo.core.api.error.ProblemDetailFactory;
import org.qavo.core.config.QavoProperties;
import org.qavo.core.feature.FeatureFlagService;
import org.qavo.core.feature.PropertyFeatureFlagService;
import org.qavo.core.observability.MdcTraceContext;
import org.qavo.core.observability.TraceContext;
import org.qavo.core.plugin.PluginRegistry;
import org.qavo.core.plugin.QavoPlugin;
import org.qavo.core.security.AnonymousSecurityContextAccessor;
import org.qavo.core.security.SecurityContextAccessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the Qavo core. Wires the platform's foundational, framework-light
 * beans following the Spring Boot principle of sensible defaults with possible override: every
 * bean is declared {@link ConditionalOnMissingBean}, so an application or another module can
 * replace any of them without disabling the rest.
 *
 * <p>This class contributes no presentation-layer machinery — the global exception handler,
 * CORS, and pagination resolvers live in {@code qavo-starter-web} — keeping the core usable in
 * non-web contexts.
 */
@AutoConfiguration
@EnableConfigurationProperties(QavoProperties.class)
public class QavoCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock qavoClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    public TraceContext traceContext() {
        return new MdcTraceContext();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProblemDetailFactory problemDetailFactory(QavoProperties properties,
                                                     TraceContext traceContext,
                                                     Clock clock) {
        return new ProblemDetailFactory(URI.create(properties.getError().getBaseUri()), traceContext, clock);
    }

    /**
     * Anonymous fallback. {@code qavo-security} supplies a Spring Security-backed accessor that,
     * being a more specific bean, replaces this one when present.
     */
    @Bean
    @ConditionalOnMissingBean
    public SecurityContextAccessor securityContextAccessor() {
        return new AnonymousSecurityContextAccessor();
    }

    /** Collects every {@link QavoPlugin} contributed by the imported plugin modules. */
    @Bean
    @ConditionalOnMissingBean
    public PluginRegistry pluginRegistry(List<QavoPlugin> plugins) {
        return new PluginRegistry(plugins);
    }

    @Bean
    @ConditionalOnMissingBean
    public FeatureFlagService featureFlagService(QavoProperties properties) {
        return new PropertyFeatureFlagService(properties);
    }
}
