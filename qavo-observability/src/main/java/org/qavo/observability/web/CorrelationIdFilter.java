/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.observability.web;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.qavo.core.observability.MdcKeys;
import org.qavo.core.security.SecurityContextAccessor;
import org.qavo.observability.config.QavoObservabilityProperties;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Establishes the correlation context for every inbound request (see architecture &sect;5.3).
 *
 * <p>The {@code traceId} is taken from an incoming W3C {@code traceparent} header when present,
 * otherwise generated. It is placed in the MDC — together with {@code appName} and, once
 * authentication has resolved, {@code userId} — so the structured logging pipeline stamps it on
 * every log line. The resolved {@code traceId} is echoed back in a response header. The MDC is
 * always cleared in a {@code finally} block to prevent leakage across pooled threads.
 *
 * <p>This filter runs at the highest precedence so the context is established before any other
 * filter logs. It is enforced, not optional: applications cannot bypass the structured logging
 * contract.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String TRACEPARENT_HEADER = "traceparent";

    private final QavoObservabilityProperties properties;
    private final SecurityContextAccessor securityContextAccessor;

    public CorrelationIdFilter(QavoObservabilityProperties properties,
                               SecurityContextAccessor securityContextAccessor) {
        this.properties = properties;
        this.securityContextAccessor = securityContextAccessor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        try {
            MDC.put(MdcKeys.TRACE_ID, traceId);
            MDC.put(MdcKeys.APP_NAME, properties.getApplicationName());
            securityContextAccessor.currentPrincipal()
                    .ifPresent(principal -> MDC.put(MdcKeys.USER_ID, principal.id()));

            response.setHeader(properties.getResponseTraceHeader(), traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MdcKeys.TRACE_ID);
            MDC.remove(MdcKeys.APP_NAME);
            MDC.remove(MdcKeys.USER_ID);
            MDC.remove(MdcKeys.SPAN_ID);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceParent = request.getHeader(TRACEPARENT_HEADER);
        String fromHeader = extractTraceId(traceParent);
        if (StringUtils.hasText(fromHeader)) {
            return fromHeader;
        }
        if (properties.isGenerateTraceIdIfMissing()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return "unknown";
    }

    /**
     * Extracts the 32-hex-character trace-id from a W3C {@code traceparent} value of the form
     * {@code version-traceid-spanid-flags}. Returns {@code null} when the value is absent or malformed.
     */
    private String extractTraceId(String traceParent) {
        if (!StringUtils.hasText(traceParent)) {
            return null;
        }
        String[] parts = traceParent.split("-");
        if (parts.length >= 2 && parts[1].length() == 32) {
            return parts[1];
        }
        return null;
    }
}
