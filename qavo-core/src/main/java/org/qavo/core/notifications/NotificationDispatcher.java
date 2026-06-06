/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.notifications;

/**
 * Application-facing facade for sending notifications. Business code depends on this single
 * interface rather than on individual {@link NotificationService} implementations: the
 * dispatcher resolves the appropriate service for the request's {@link NotificationChannel},
 * invokes it, records metrics, and returns the {@link NotificationResult} to the caller.
 *
 * <p>The dispatcher contract is intentionally fail-soft: {@link #dispatch(NotificationRequest)}
 * never throws on delivery failure. A failed delivery is observable both via the returned
 * result and via the {@code qavo.notifications.sent} counter with {@code status=failure}.
 */
public interface NotificationDispatcher {

    /**
     * Dispatch the request to a matching provider. Returns a
     * {@link NotificationResult#failure(String)} when no provider supports the requested
     * channel; never returns {@code null}.
     */
    NotificationResult dispatch(NotificationRequest request);
}
