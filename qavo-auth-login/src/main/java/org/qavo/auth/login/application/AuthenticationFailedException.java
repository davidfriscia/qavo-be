/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.application;

import org.qavo.core.api.error.CoreProblemType;
import org.qavo.core.domain.exception.QavoException;

/**
 * Raised when local credential authentication fails. Maps to HTTP 401 via the global handler.
 * The message is deliberately generic to avoid disclosing whether the username exists.
 */
public class AuthenticationFailedException extends QavoException {

    public AuthenticationFailedException() {
        super(CoreProblemType.UNAUTHORIZED, "Invalid username or password");
    }
}
