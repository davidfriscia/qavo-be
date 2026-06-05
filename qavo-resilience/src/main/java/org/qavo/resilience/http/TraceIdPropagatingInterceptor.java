/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.resilience.http;

import java.io.IOException;

import org.qavo.core.observability.TraceContext;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Forwards the current request's correlation identifier as a header on every outbound call so
 * downstream services see — and can log — the same {@code traceId} carried in this process's
 * MDC. When no trace is in scope (e.g. a startup probe or a scheduled job that has not opened
 * one) the interceptor leaves the header untouched.
 */
public class TraceIdPropagatingInterceptor implements ClientHttpRequestInterceptor {

    private final TraceContext traceContext;
    private final String headerName;

    public TraceIdPropagatingInterceptor(TraceContext traceContext, String headerName) {
        this.traceContext = traceContext;
        this.headerName = headerName;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        String traceId = traceContext.currentTraceId();
        if (traceId != null && !traceId.isBlank() && !request.getHeaders().containsKey(headerName)) {
            request.getHeaders().add(headerName, traceId);
        }
        return execution.execute(request, body);
    }
}
