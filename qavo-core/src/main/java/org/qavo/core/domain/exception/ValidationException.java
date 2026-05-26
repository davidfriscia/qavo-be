/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.domain.exception;

import java.util.List;

import org.qavo.core.api.error.CoreProblemType;
import org.qavo.core.api.error.FieldErrorDetail;

/**
 * Signals an application-level validation failure raised outside the declarative Bean
 * Validation pipeline (which is handled automatically). Carries field-level details that the
 * global handler surfaces in the {@code errors} array. Maps to HTTP 400.
 */
public class ValidationException extends QavoException {

    public ValidationException(String message, List<FieldErrorDetail> fieldErrors) {
        super(CoreProblemType.VALIDATION, message, fieldErrors, null);
    }

    public ValidationException(String message) {
        this(message, List.of());
    }
}
