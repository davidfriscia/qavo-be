/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.registration;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.qavo.core.api.error.CoreProblemType;
import org.qavo.core.domain.exception.QavoException;

/**
 * Raised by the registration flow when the configured rolling-window cap has been reached.
 * Mapped to {@code 503 Service Unavailable} by the global exception handler with the
 * RFC 9457 extensions {@code opensAt} (ISO-8601 UTC instant) and {@code retryAfter}
 * (whole seconds). The HTTP {@code Retry-After} response header is set to the same value.
 *
 * <p>The choice of {@code 503} over {@code 429} is deliberate (ADR 0012): {@code 429} signals
 * a per-client rate-limit decision, whereas this is a global capacity decision independent of
 * the requesting client.
 */
public class RegistrationCapExceededException extends QavoException {

    private final transient RegistrationCapStatus status;

    public RegistrationCapExceededException(RegistrationCapStatus status) {
        super(CoreProblemType.REGISTRATION_CAP_EXCEEDED,
                "The maximum number of registrations for the current period has been reached.");
        this.status = status;
    }

    public RegistrationCapStatus getStatus() {
        return status;
    }

    /** Whole seconds between {@code checkedAt} and {@code opensAt}; never negative. */
    public long getRetryAfterSeconds() {
        if (status == null || status.opensAt() == null || status.checkedAt() == null) {
            return 0L;
        }
        long seconds = Duration.between(status.checkedAt(), status.opensAt()).getSeconds();
        return Math.max(0L, seconds);
    }

    @Override
    public Map<String, Object> getProblemProperties() {
        Map<String, Object> props = new HashMap<>();
        Instant opensAt = status != null ? status.opensAt() : null;
        if (opensAt != null) {
            props.put("opensAt", opensAt.toString());
        }
        props.put("retryAfter", getRetryAfterSeconds());
        return props;
    }

    /** HTTP headers merged into the error response (e.g. {@code Retry-After}). */
    @Override
    public Map<String, String> getResponseHeaders() {
        return Map.of("Retry-After", Long.toString(getRetryAfterSeconds()));
    }
}
