/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.application;

import org.qavo.core.api.error.CoreProblemType;
import org.qavo.core.domain.exception.QavoException;

/**
 * Raised on attempts to verify with a token whose row already has {@code consumed=true}.
 * Mapped to a 400 RFC 9457 response with {@code type=verification-token-already-used}.
 */
public class VerificationTokenAlreadyUsedException extends QavoException {

    public VerificationTokenAlreadyUsedException() {
        super(CoreProblemType.VERIFICATION_TOKEN_ALREADY_USED,
                "The verification token has already been used");
    }
}
