/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.notifications.provider;

import org.qavo.core.notifications.NotificationChannel;
import org.qavo.core.notifications.NotificationRequest;
import org.qavo.core.notifications.NotificationResult;
import org.qavo.core.notifications.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Catch-all {@link NotificationService} used when no provider for the requested channel is
 * configured. Logging the suppressed dispatch keeps the omission observable without forcing
 * the calling business code to be aware of the misconfiguration; this is the design tradeoff
 * documented in ADR 0010 (notifications are fail-soft).
 *
 * <p>Reports support for any {@link NotificationChannel} so the dispatcher can always find a
 * fallback. Configured providers always win precedence ahead of this one (see
 * {@code DefaultNotificationDispatcher}).
 */
public class NoOpNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NoOpNotificationService.class);

    @Override
    public NotificationResult send(NotificationRequest request) {
        log.warn(
                "Notification dropped: no provider configured for channel={} recipient={}",
                request.channel(), request.recipient());
        return NotificationResult.success("noop");
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return true;
    }
}
