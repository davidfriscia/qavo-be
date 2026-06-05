/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.resilience.http;

import java.util.Map;
import java.util.function.Consumer;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * Outbound HTTP entry point used by Qavo applications and plugins to call other services
 * (architecture &sect;5.7). Wraps Spring's {@code RestClient} so resilience behaviors — retry,
 * circuit breaker, request/response logging, and {@code traceId} propagation — apply uniformly
 * without each call site repeating boilerplate.
 *
 * <p>The platform exposes one client per logical "backend" (i.e. one {@code QavoHttpClient}
 * bean per Resilience4j instance name), so policies tune at the right granularity: a flaky
 * search service can have an aggressive retry policy while a payment provider has none.
 */
public interface QavoHttpClient {

    /** The logical name of this client, used to look up Resilience4j and metric configuration. */
    String name();

    /**
     * Execute an HTTP GET against {@code path} (relative to this client's base URL) and
     * deserialize the body into {@code responseType}.
     */
    <T> ResponseEntity<T> get(String path, Class<T> responseType, Consumer<HttpHeaders> headers);

    /** Variant supporting generic response types (e.g. {@code List<Foo>}). */
    <T> ResponseEntity<T> get(String path, ParameterizedTypeReference<T> responseType,
                              Consumer<HttpHeaders> headers);

    /** Execute an HTTP POST with a JSON-serializable body. */
    <T> ResponseEntity<T> post(String path, Object body, Class<T> responseType,
                               Consumer<HttpHeaders> headers);

    /** Convenience: GET with no header customization. */
    default <T> ResponseEntity<T> get(String path, Class<T> responseType) {
        return get(path, responseType, headers -> { });
    }

    /** Convenience: POST with no header customization. */
    default <T> ResponseEntity<T> post(String path, Object body, Class<T> responseType) {
        return post(path, body, responseType, headers -> { });
    }

    /** Snapshot of the runtime metadata (configured base URL, request defaults). */
    Map<String, String> info();
}
