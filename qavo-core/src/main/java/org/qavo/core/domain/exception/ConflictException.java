/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.domain.exception;

import org.qavo.core.api.error.CoreProblemType;

/**
 * Signals a conflict with the current state of a resource, such as a uniqueness violation or a
 * concurrent-modification clash. Maps to HTTP 409.
 */
public class ConflictException extends QavoException {

    public ConflictException(String message) {
        super(CoreProblemType.CONFLICT, message);
    }
}
