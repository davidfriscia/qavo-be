/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.registration;

import java.time.Duration;
import java.time.Instant;

/**
 * Immutable snapshot of the registration cap state at a given instant. Produced by
 * {@link RegistrationCapService#checkCap()}; consumed by the registration controller, the
 * read-only registration-status endpoint, and the global exception handler when the cap is
 * exceeded.
 *
 * @param open             true if new registrations are currently permitted.
 * @param currentCount     number of registrations counted within the current window.
 * @param maxRegistrations the configured cap (0 when the cap feature is disabled).
 * @param windowDuration   the configured rolling window (null when disabled).
 * @param opensAt          the instant at which the cap will reopen, i.e. when the oldest
 *                         registration in the window slides out. Null when {@code open} is
 *                         true or when the cap is disabled.
 * @param checkedAt        the instant at which this status was evaluated.
 */
public record RegistrationCapStatus(
        boolean open,
        int currentCount,
        int maxRegistrations,
        Duration windowDuration,
        Instant opensAt,
        Instant checkedAt) {
}
