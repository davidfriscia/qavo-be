/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.api;

import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JSON shape returned by {@code GET /api/v1/auth/registration-status}. Fields beyond
 * {@code open} and {@code checkedAt} are present only when the cap is closed; they are
 * omitted (rather than emitted as {@code null}) so the open-case payload stays minimal.
 *
 * @param open             true when new registrations are currently accepted.
 * @param currentCount     number of registrations counted within the active window (closed case only).
 * @param maxRegistrations configured cap (closed case only).
 * @param windowDuration   configured rolling window, ISO-8601 (closed case only).
 * @param opensAt          UTC instant at which the cap is expected to reopen (closed case only).
 * @param retryAfter       whole seconds between {@code checkedAt} and {@code opensAt} (closed case only).
 * @param checkedAt        UTC instant at which this status was computed.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "RegistrationStatus",
        description = "Snapshot of whether self-service registration is currently accepted.")
public record RegistrationStatusResponse(
        @Schema(description = "True when new registrations are currently accepted.", example = "true")
        boolean open,
        @Schema(description = "Registrations counted within the active rolling window. "
                + "Present only when open=false.", example = "100")
        Integer currentCount,
        @Schema(description = "Configured maximum number of registrations within the window. "
                + "Present only when open=false.", example = "100")
        Integer maxRegistrations,
        @Schema(description = "Configured rolling window as an ISO-8601 duration. "
                + "Present only when open=false.", example = "PT24H")
        Duration windowDuration,
        @Schema(description = "UTC instant at which the cap is expected to reopen. "
                + "Present only when open=false.", example = "2026-06-07T10:30:00Z")
        Instant opensAt,
        @Schema(description = "Whole seconds between checkedAt and opensAt. "
                + "Present only when open=false.", example = "5400")
        Long retryAfter,
        @Schema(description = "UTC instant at which this status was evaluated.",
                example = "2026-06-07T09:00:00Z")
        Instant checkedAt) {

    /** Convenience factory for the open / cap-disabled case. */
    public static RegistrationStatusResponse open(Instant checkedAt) {
        return new RegistrationStatusResponse(true, null, null, null, null, null, checkedAt);
    }
}
