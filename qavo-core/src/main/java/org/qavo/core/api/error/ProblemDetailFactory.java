/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.api.error;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.qavo.core.observability.TraceContext;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;

/**
 * Builds RFC 9457 {@link ProblemDetail} responses in the single, consistent shape mandated by
 * the platform (see architecture &sect;5.2). Every error returned by a Qavo-based application
 * flows through this factory, so the {@code type}, {@code title}, {@code timestamp},
 * {@code traceId} and {@code errors} extensions are always populated the same way.
 *
 * <p>This factory is framework-light on purpose: it depends only on the error base URI and a
 * {@link TraceContext}. The global {@code @RestControllerAdvice} that wires it lives in the web
 * starter, keeping the core free of presentation-layer machinery.
 */
public class ProblemDetailFactory {

    /** Extension member names. Kept here so producers and consumers agree on the contract. */
    public static final String TIMESTAMP = "timestamp";
    public static final String TRACE_ID = "traceId";
    public static final String CODE = "code";
    public static final String ERRORS = "errors";

    private final URI errorBaseUri;
    private final TraceContext traceContext;
    private final Clock clock;

    public ProblemDetailFactory(URI errorBaseUri, TraceContext traceContext, Clock clock) {
        this.errorBaseUri = errorBaseUri;
        this.traceContext = traceContext;
        this.clock = clock;
    }

    /** Creates a problem detail for the given type with the type's default title and status. */
    public ProblemDetail create(ProblemType type, String detail) {
        return create(type, type.defaultTitle(), detail, null);
    }

    /** Creates a problem detail, optionally attaching field-level validation errors. */
    public ProblemDetail create(ProblemType type, String title, String detail,
                                @Nullable List<FieldErrorDetail> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(type.status(), detail);
        problem.setType(typeUri(type));
        problem.setTitle(title);
        problem.setProperty(CODE, type.code());
        problem.setProperty(TIMESTAMP, Instant.now(clock));
        problem.setProperty(TRACE_ID, traceContext.currentTraceId());
        if (errors != null && !errors.isEmpty()) {
            problem.setProperty(ERRORS, errors);
        }
        return problem;
    }

    /**
     * Adds the platform's standard {@code timestamp} and {@code traceId} extensions to a
     * {@link ProblemDetail} that was produced elsewhere (for example by Spring's framework-level
     * exception handling), without overwriting any value already present. Keeps even framework
     * errors consistent with the platform contract.
     */
    public void enrich(ProblemDetail problem) {
        if (problem.getProperties() == null || !problem.getProperties().containsKey(TIMESTAMP)) {
            problem.setProperty(TIMESTAMP, Instant.now(clock));
        }
        if (problem.getProperties() == null || !problem.getProperties().containsKey(TRACE_ID)) {
            problem.setProperty(TRACE_ID, traceContext.currentTraceId());
        }
    }

    /** Resolves the absolute {@code type} URI for a problem type against the configured base. */
    public URI typeUri(ProblemType type) {
        String base = errorBaseUri.toString();
        String separator = base.endsWith("/") ? "" : "/";
        return URI.create(base + separator + type.code());
    }
}
