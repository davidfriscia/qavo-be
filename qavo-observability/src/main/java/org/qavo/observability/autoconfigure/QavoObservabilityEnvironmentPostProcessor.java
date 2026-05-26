/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.observability.autoconfigure;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Contributes the platform's observability defaults as a low-precedence property source, so an
 * application inherits secure, sensible Actuator and tracing settings out of the box while
 * remaining free to override any of them in its own {@code application.yml} (see architecture
 * &sect;5.3, &sect;5.6).
 *
 * <p>Defaults: expose only {@code health}, {@code info}, {@code metrics} and {@code prometheus};
 * show health details only to authorized users; enable liveness/readiness probe groups; and tag
 * every metric with the application name for Grafana dashboards.
 */
public class QavoObservabilityEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "qavoObservabilityDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("management.endpoints.web.exposure.include", "health,info,metrics,prometheus");
        defaults.put("management.endpoint.health.show-details", "when-authorized");
        defaults.put("management.endpoint.health.probes.enabled", "true");
        defaults.put("management.health.livenessstate.enabled", "true");
        defaults.put("management.health.readinessstate.enabled", "true");
        defaults.put("management.metrics.tags.application", "${qavo.observability.application-name:qavo-app}");
        // Conservative trace sampling default; raise per environment as needed.
        defaults.put("management.tracing.sampling.probability", "0.1");
        // Recognize a TLS-terminating reverse proxy so URLs/redirects reflect HTTPS (architecture §5.5).
        defaults.put("server.forward-headers-strategy", "framework");

        environment.getPropertySources()
                .addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }

    @Override
    public int getOrder() {
        // Run late so these act as defaults that explicit configuration overrides.
        return Ordered.LOWEST_PRECEDENCE - 10;
    }
}
