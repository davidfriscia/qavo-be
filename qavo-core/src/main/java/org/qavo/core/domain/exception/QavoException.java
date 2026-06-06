/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.domain.exception;

import java.util.List;
import java.util.Map;

import org.qavo.core.api.error.CoreProblemType;
import org.qavo.core.api.error.FieldErrorDetail;
import org.qavo.core.api.error.ProblemType;

/**
 * Root of the platform exception hierarchy. Carries a {@link ProblemType} so the global
 * exception handler can translate any platform or application exception into the standard
 * RFC 9457 response without a per-type {@code instanceof} ladder (see architecture &sect;5.2).
 *
 * <p>Applications extend the concrete subclasses (or this class) and may supply their own
 * {@link ProblemType} implementations for domain-specific conditions.
 */
public abstract class QavoException extends RuntimeException {

    private final transient ProblemType problemType;
    private final transient List<FieldErrorDetail> fieldErrors;

    protected QavoException(ProblemType problemType, String message) {
        this(problemType, message, List.of(), null);
    }

    protected QavoException(ProblemType problemType, String message, Throwable cause) {
        this(problemType, message, List.of(), cause);
    }

    protected QavoException(ProblemType problemType, String message,
                            List<FieldErrorDetail> fieldErrors, Throwable cause) {
        super(message, cause);
        this.problemType = problemType != null ? problemType : CoreProblemType.INTERNAL_ERROR;
        this.fieldErrors = fieldErrors != null ? List.copyOf(fieldErrors) : List.of();
    }

    /** The problem classification used to render the error response. */
    public ProblemType getProblemType() {
        return problemType;
    }

    /** Field-level details, empty unless this is a validation-style failure. */
    public List<FieldErrorDetail> getFieldErrors() {
        return fieldErrors;
    }

    /**
     * Extension members merged into the Problem Details body by the global exception handler.
     * Subclasses override to expose machine-readable specifics (e.g. {@code unlocksAt}) without
     * needing a dedicated handler. Returns an empty map by default so the contract stays
     * predictable.
     */
    public Map<String, Object> getProblemProperties() {
        return Map.of();
    }

    /**
     * HTTP response headers the global exception handler must set on the error response.
     * Subclasses override to add transport-level metadata such as {@code Retry-After} that
     * HTTP-aware clients and proxies act on without parsing the body. Returns an empty map by
     * default; never {@code null}.
     */
    public Map<String, String> getResponseHeaders() {
        return Map.of();
    }
}
