/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.resilience.autoconfigure;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;

import org.qavo.core.observability.TraceContext;
import org.qavo.resilience.http.DefaultQavoHttpClient;
import org.qavo.resilience.http.QavoHttpClient;
import org.qavo.resilience.http.QavoHttpClientProperties;
import org.qavo.resilience.http.QavoHttpClientRegistry;
import org.qavo.resilience.http.TraceIdPropagatingInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * Wires the resilient outbound HTTP client (architecture &sect;5.7). Activates whenever the
 * Resilience4j Spring Boot 3 integration is on the classpath; Resilience4j's own
 * {@code CircuitBreakerAutoConfiguration} / {@code RetryAutoConfiguration} build the registries
 * and we run after them so the registries are available for client construction.
 *
 * <p>One {@link QavoHttpClient} is created for each entry in {@code qavo.resilience.http.clients};
 * its circuit-breaker and retry instances are looked up from the registries by client name, so
 * operators tune policies under the documented {@code resilience4j.circuitbreaker.instances.<name>}
 * and {@code resilience4j.retry.instances.<name>} keys.
 */
@AutoConfiguration(after = {CircuitBreakerAutoConfiguration.class, RetryAutoConfiguration.class})
@ConditionalOnClass({RestClient.class, CircuitBreakerRegistry.class, RetryRegistry.class})
@EnableConfigurationProperties(QavoHttpClientProperties.class)
public class QavoHttpClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TraceIdPropagatingInterceptor qavoTraceIdInterceptor(TraceContext traceContext,
                                                                QavoHttpClientProperties properties) {
        return new TraceIdPropagatingInterceptor(traceContext, properties.getTraceHeader());
    }

    @Bean
    @ConditionalOnMissingBean
    public QavoHttpClientRegistry qavoHttpClientRegistry(QavoHttpClientProperties properties,
                                                         CircuitBreakerRegistry circuitBreakerRegistry,
                                                         RetryRegistry retryRegistry,
                                                         TraceIdPropagatingInterceptor interceptor) {
        Map<String, QavoHttpClient> clients = new LinkedHashMap<>();
        properties.getClients().forEach((name, cfg) -> {
            RestClient.Builder builder = RestClient.builder().requestInterceptor(interceptor);
            if (cfg.getBaseUrl() != null && !cfg.getBaseUrl().isBlank()) {
                builder = builder.baseUrl(cfg.getBaseUrl());
            }
            clients.put(name, new DefaultQavoHttpClient(
                    name,
                    builder.build(),
                    circuitBreakerRegistry.circuitBreaker(name),
                    retryRegistry.retry(name),
                    cfg.getBaseUrl()));
        });
        return new QavoHttpClientRegistry(clients);
    }
}
