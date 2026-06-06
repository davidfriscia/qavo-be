/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.notifications;

/**
 * Outcome of a single {@link NotificationService#send(NotificationRequest) dispatch}. Failures
 * are returned, never thrown — notification delivery is a soft-failure concern and the calling
 * business operation must not be blocked by an unreachable SMTP server or rate-limited bot
 * API. The {@code providerMessageId} surfaces the identifier the upstream provider assigned to
 * the message when it is meaningful and available; the {@code errorMessage} is populated on
 * failure and intended for diagnostics, not end-user display.
 */
public record NotificationResult(
        boolean success,
        String providerMessageId,
        String errorMessage) {

    /** Convenience builder for a successful dispatch carrying (or omitting via {@code null}) the provider id. */
    public static NotificationResult success(String providerMessageId) {
        return new NotificationResult(true, providerMessageId, null);
    }

    /** Convenience builder for a failed dispatch carrying the diagnostic message. */
    public static NotificationResult failure(String errorMessage) {
        return new NotificationResult(false, null, errorMessage);
    }
}
