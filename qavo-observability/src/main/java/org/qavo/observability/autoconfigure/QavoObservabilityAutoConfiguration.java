/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.observability.autoconfigure;

import org.qavo.core.autoconfigure.QavoCoreAutoConfiguration;
import org.qavo.core.security.SecurityContextAccessor;
import org.qavo.observability.config.QavoObservabilityProperties;
import org.qavo.observability.web.CorrelationIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for observability. Registers the correlation-id filter at the highest
 * precedence so the trace context is established before anything else runs. Metrics, tracing
 * export, and Actuator endpoints are configured via the {@code QavoObservabilityEnvironmentPostProcessor}
 * defaults, all overridable by the application.
 *
 * <p>The filter is published as a plain {@link CorrelationIdFilter} bean (ordered via its
 * {@code @Order}) rather than wrapped in a {@code FilterRegistrationBean}: Spring Boot auto-registers
 * {@code Filter} beans in the servlet container, and {@code @AutoConfigureMockMvc} also applies them,
 * so the trace context is present in both production and slice tests.
 */
@AutoConfiguration(after = QavoCoreAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(QavoObservabilityProperties.class)
public class QavoObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CorrelationIdFilter qavoCorrelationIdFilter(QavoObservabilityProperties properties,
                                                       SecurityContextAccessor securityContextAccessor) {
        return new CorrelationIdFilter(properties, securityContextAccessor);
    }
}
