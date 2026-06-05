/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.resilience.http;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * Default {@link QavoHttpClient} implementation. Each instance owns one {@link RestClient}
 * pre-configured with the base URL, traceId interceptor, and a fixed name shared with its
 * Resilience4j {@link CircuitBreaker} and {@link Retry} instances. The decorators are applied
 * <strong>around</strong> the call (not via aspect) so the same code path is exercised in tests
 * and in production, and so failures are correctly classified by Resilience4j as exceptions
 * rather than swallowed by AOP wiring.
 */
public class DefaultQavoHttpClient implements QavoHttpClient {

    private final String name;
    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final String baseUrl;

    public DefaultQavoHttpClient(String name, RestClient restClient,
                                 CircuitBreaker circuitBreaker, Retry retry,
                                 String baseUrl) {
        this.name = name;
        this.restClient = restClient;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.baseUrl = baseUrl;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public <T> ResponseEntity<T> get(String path, Class<T> responseType, Consumer<HttpHeaders> headers) {
        return execute(() -> restClient.get()
                .uri(path)
                .headers(headers)
                .retrieve()
                .toEntity(responseType));
    }

    @Override
    public <T> ResponseEntity<T> get(String path, ParameterizedTypeReference<T> responseType,
                                     Consumer<HttpHeaders> headers) {
        return execute(() -> restClient.get()
                .uri(path)
                .headers(headers)
                .retrieve()
                .toEntity(responseType));
    }

    @Override
    public <T> ResponseEntity<T> post(String path, Object body, Class<T> responseType,
                                      Consumer<HttpHeaders> headers) {
        return execute(() -> restClient.post()
                .uri(path)
                .headers(headers)
                .body(body)
                .retrieve()
                .toEntity(responseType));
    }

    @Override
    public Map<String, String> info() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("name", name);
        info.put("baseUrl", baseUrl == null ? "" : baseUrl);
        info.put("circuitBreakerState", circuitBreaker.getState().name());
        return Map.copyOf(info);
    }

    /**
     * Decorate the supplier with retry (innermost) and circuit breaker (outermost) so retries
     * happen first and the breaker only sees a final terminal outcome — matching the documented
     * Resilience4j ordering and producing correct circuit-breaker call-not-permitted counts.
     */
    private <T> T execute(Supplier<T> supplier) {
        Supplier<T> retried = Retry.decorateSupplier(retry, supplier);
        return circuitBreaker.executeSupplier(retried);
    }
}
