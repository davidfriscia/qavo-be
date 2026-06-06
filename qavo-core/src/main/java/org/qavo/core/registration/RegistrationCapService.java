/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.registration;

/**
 * Tracks and enforces a global, rolling-window cap on the number of new user registrations
 * accepted by the platform. The interface lives in {@code qavo-core} so any consumer can
 * inject and depend on it without taking a hard dependency on {@code qavo-auth-registration}
 * (architecture §6 — pluggable capabilities; cross-cutting SPIs live in the core).
 *
 * <p>Implementations must be thread-safe. The platform provides two implementations: a
 * database-backed default in {@code qavo-auth-registration} and a {@code NoOpRegistrationCapService}
 * that is registered automatically when the feature is disabled. See ADR 0012 for the design
 * rationale.
 */
public interface RegistrationCapService {

    /**
     * Returns the current cap state without modifying it.
     *
     * @return a {@link RegistrationCapStatus} describing whether registration is currently
     *         open and, when closed, when the cap is expected to reopen.
     */
    RegistrationCapStatus checkCap();

    /**
     * Records a successful registration event at the current instant. Implementations should
     * persist the event durably so that the count survives restarts and is correct across
     * application instances.
     *
     * <p>Implementations must be idempotent with respect to the window count: calling this
     * twice for the same {@code userId} must not double-count the user — implementations may
     * rely on the unique user id to deduplicate when needed.
     *
     * @param userId stable, unique identifier of the newly registered user; never {@code null}.
     */
    void recordRegistration(String userId);
}
