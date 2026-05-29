/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.domain.exception;

import org.qavo.core.api.error.CoreProblemType;
import org.qavo.core.api.error.ProblemType;

/**
 * Signals a violation of a business invariant — the request was well-formed and authorized,
 * but it cannot be satisfied given the current domain state. Maps by default to HTTP 422.
 *
 * <p>Applications typically subclass this with a domain-specific {@link ProblemType}.
 */
public class BusinessException extends QavoException {

    public BusinessException(String message) {
        super(CoreProblemType.BUSINESS_RULE, message);
    }

    public BusinessException(ProblemType problemType, String message) {
        super(problemType, message);
    }

    public BusinessException(ProblemType problemType, String message, Throwable cause) {
        super(problemType, message, cause);
    }
}
