/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.resilience.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qavo.resilience.autoconfigure.QavoHttpClientAutoConfiguration;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Exercises the resilience contract end-to-end against a WireMock stub: retries are honored on
 * transient failures, the circuit breaker opens after the configured failure ratio is exceeded,
 * and the configured trace header is propagated on every outbound call. WireMock runs on an
 * ephemeral port so tests are concurrency-safe.
 */
@org.junit.jupiter.api.extension.ExtendWith(QavoHttpClientIT.MdcCleanup.class)
class QavoHttpClientIT {

    /** Clears MDC after each test so the trace-header assertions cannot leak across tests. */
    static class MdcCleanup implements org.junit.jupiter.api.extension.AfterEachCallback {
        @Override public void afterEach(org.junit.jupiter.api.extension.ExtensionContext context) {
            MDC.clear();
        }
    }

    private WireMockServer wireMock;
    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration.class,
                        io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration.class,
                        QavoHttpClientAutoConfiguration.class))
                .withUserConfiguration(TestTraceContextConfig.class)
                .withPropertyValues(
                        "qavo.resilience.http.clients.demo.base-url=" + wireMock.baseUrl(),
                        // Two attempts total, so a single transient 503 followed by a 200 succeeds.
                        "resilience4j.retry.instances.demo.max-attempts=2",
                        "resilience4j.retry.instances.demo.wait-duration=PT0S",
                        "resilience4j.retry.instances.demo.retry-exceptions[0]=org.springframework.web.client.HttpServerErrorException",
                        // Tight breaker so the open-state test does not need many iterations.
                        "resilience4j.circuitbreaker.instances.demo.sliding-window-size=4",
                        "resilience4j.circuitbreaker.instances.demo.minimum-number-of-calls=4",
                        "resilience4j.circuitbreaker.instances.demo.failure-rate-threshold=50",
                        "resilience4j.circuitbreaker.instances.demo.permitted-number-of-calls-in-half-open-state=1");
    }

    @AfterEach
    void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @Test
    void retriesTransientServerErrorAndPropagatesTraceHeader() {
        MDC.put("traceId", "trace-abc-123");
        wireMock.stubFor(get(urlEqualTo("/widgets/1"))
                .inScenario("retry")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("recovered"));
        wireMock.stubFor(get(urlEqualTo("/widgets/1"))
                .inScenario("retry")
                .whenScenarioStateIs("recovered")
                .willReturn(aResponse().withStatus(200).withBody("{\"id\":1}")));

        contextRunner.run(context -> {
            QavoHttpClient client = context.getBean(QavoHttpClientRegistry.class).get("demo");
            String body = client.get("/widgets/1", String.class).getBody();
            assertThat(body).contains("\"id\":1");
        });

        // Verify the trace header rode along on both attempts.
        wireMock.verify(2, getRequestedFor(urlEqualTo("/widgets/1"))
                .withHeader("X-Trace-Id", equalTo("trace-abc-123")));
    }

    @Test
    void opensCircuitBreakerAfterRepeatedFailures() {
        wireMock.stubFor(get(urlEqualTo("/widgets/2"))
                .willReturn(aResponse().withStatus(500)));

        contextRunner.run(context -> {
            QavoHttpClient client = context.getBean(QavoHttpClientRegistry.class).get("demo");
            // Drive enough failed calls to satisfy minimum-number-of-calls and trip the breaker.
            for (int i = 0; i < 4; i++) {
                try {
                    client.get("/widgets/2", String.class);
                } catch (Exception expected) {
                    // Expected: upstream 500s after exhausting retries surface as HttpServerErrorException.
                }
            }
            // The next call must be short-circuited by the now-open breaker.
            assertThatThrownBy(() -> client.get("/widgets/2", String.class))
                    .isInstanceOf(CallNotPermittedException.class);
        });
    }

    /**
     * Minimal {@code TraceContext} stub exposing MDC's {@code traceId}. The full
     * {@code QavoCoreAutoConfiguration} brings transitive dependencies that this slice does not
     * need; isolating to a one-line stub keeps the test self-contained.
     */
    @org.springframework.boot.test.context.TestConfiguration
    static class TestTraceContextConfig {
        @org.springframework.context.annotation.Bean
        org.qavo.core.observability.TraceContext traceContext() {
            return new org.qavo.core.observability.TraceContext() {
                @Override public String currentTraceId() { return MDC.get("traceId"); }
                @Override public String currentSpanId() { return null; }
            };
        }
    }
}
