/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.jwt;

import org.qavo.core.api.error.CoreProblemType;
import org.qavo.core.domain.exception.QavoException;

/**
 * Raised when a refresh token is missing, expired, revoked, or otherwise unusable. Maps to HTTP
 * 401 via the global handler so the wire shape matches a failed login.
 */
public class RefreshTokenException extends QavoException {

    public RefreshTokenException() {
        super(CoreProblemType.UNAUTHORIZED, "Refresh token is invalid or has expired");
    }
}
