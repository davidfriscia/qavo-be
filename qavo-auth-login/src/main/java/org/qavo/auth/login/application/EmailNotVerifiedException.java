/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.application;

import org.qavo.core.api.error.CoreProblemType;
import org.qavo.core.domain.exception.QavoException;

/**
 * Raised by the login flow when the configured
 * {@code qavo.auth.registration.email-verification.require-verified-email-to-login=true} guard
 * blocks a credential exchange because the authenticated principal's {@code emailVerified}
 * flag is still false. Translated by the platform error handler to a 403 RFC 9457 response with
 * {@code type=email-not-verified}; the end-user is expected to complete verification (via the
 * link or the resend endpoint) and retry.
 */
public class EmailNotVerifiedException extends QavoException {

    public EmailNotVerifiedException() {
        super(CoreProblemType.EMAIL_NOT_VERIFIED, "Email address is not verified");
    }
}
