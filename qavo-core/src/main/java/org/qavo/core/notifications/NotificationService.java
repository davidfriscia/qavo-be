/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.notifications;

/**
 * Provider-side SPI for a single notification channel. Implementations are stateless beans
 * registered via Spring auto-configuration and are looked up by the
 * {@link NotificationDispatcher} on demand. Each service declares the {@link
 * NotificationChannel} it handles via {@link #supports(NotificationChannel)}; the dispatcher
 * delegates a request to the first service whose {@code supports} returns {@code true} (see
 * {@code DefaultNotificationDispatcher} for the precedence rules).
 *
 * <p>Implementations should never throw on transport failures. The {@link
 * NotificationResult#failure(String)} channel is the contract for delivering diagnostics
 * back to the caller.
 */
public interface NotificationService {

    /**
     * Deliver the request via this provider. Implementations must catch transport-level
     * exceptions and translate them into a {@code NotificationResult.failure(...)} value;
     * the only acceptable thrown exceptions are programmer errors (e.g. {@code null}
     * arguments) detected before any network I/O.
     */
    NotificationResult send(NotificationRequest request);

    /** {@code true} when this provider can deliver the given channel. */
    boolean supports(NotificationChannel channel);
}
