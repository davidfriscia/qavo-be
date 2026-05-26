/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.observability;

/**
 * Read access to the current request's correlation identifiers.
 *
 * <p>Exposed as an abstraction so that components needing the {@code traceId} (notably the
 * {@link org.qavo.core.api.error.ProblemDetailFactory}) do not bind directly to a logging or
 * tracing implementation. The default core implementation reads from MDC; the observability
 * module may supply a richer implementation backed by the active tracer.
 */
public interface TraceContext {

    /** The current trace identifier, or {@code null} if no request is in scope. */
    String currentTraceId();

    /** The current span identifier, or {@code null} if not available. */
    String currentSpanId();
}
