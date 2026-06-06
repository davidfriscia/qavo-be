/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.application;

import org.qavo.core.api.error.CoreProblemType;
import org.qavo.core.domain.exception.QavoException;

/**
 * Raised when a token's {@code expires_at} is at or before now. Translated into a 400 RFC 9457
 * response with {@code type=verification-token-expired}; the end-user can request a fresh token
 * via the resend endpoint.
 */
public class VerificationTokenExpiredException extends QavoException {

    public VerificationTokenExpiredException() {
        super(CoreProblemType.VERIFICATION_TOKEN_EXPIRED, "The verification token has expired");
    }
}
