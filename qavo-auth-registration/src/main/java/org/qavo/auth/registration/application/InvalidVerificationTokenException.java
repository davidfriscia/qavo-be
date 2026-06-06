/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.application;

import org.qavo.core.api.error.CoreProblemType;
import org.qavo.core.domain.exception.QavoException;

/**
 * Raised when a presented email-verification token does not match any persisted row. The user
 * never sees a stack trace; the global exception handler translates this into a 400 RFC 9457
 * response with {@code type=invalid-verification-token}.
 */
public class InvalidVerificationTokenException extends QavoException {

    public InvalidVerificationTokenException() {
        super(CoreProblemType.INVALID_VERIFICATION_TOKEN, "The verification token is invalid");
    }
}
