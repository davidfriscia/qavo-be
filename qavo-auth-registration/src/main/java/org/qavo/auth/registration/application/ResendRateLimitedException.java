/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.application;

import java.util.Map;

import org.qavo.core.api.error.CoreProblemType;
import org.qavo.core.domain.exception.QavoException;

/**
 * Raised when the configured per-hour resend ceiling has been reached for a user. Mapped to a
 * 429 RFC 9457 response with {@code type=resend-rate-limited}; the {@code retryAfterSeconds}
 * extension member tells the client how long to wait before another resend will be considered.
 */
public class ResendRateLimitedException extends QavoException {

    private final long retryAfterSeconds;

    public ResendRateLimitedException(long retryAfterSeconds) {
        super(CoreProblemType.RESEND_RATE_LIMITED,
                "Too many verification email resends; please wait before retrying");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    @Override
    public Map<String, Object> getProblemProperties() {
        return Map.of("retryAfterSeconds", retryAfterSeconds);
    }
}
