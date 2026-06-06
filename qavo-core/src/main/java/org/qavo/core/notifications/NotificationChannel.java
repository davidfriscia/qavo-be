/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.notifications;

/**
 * Transport over which a {@link NotificationRequest} is delivered. Channels are intentionally
 * coarse-grained: each value represents a class of provider (email, instant messaging, no-op)
 * rather than a specific vendor, so the same {@link NotificationDispatcher} contract can route
 * to any compatible implementation without leaking provider names into application code.
 *
 * <p>{@link #NONE} is the sentinel channel handled by the no-op fallback service; it ensures the
 * dispatcher always has at least one provider to delegate to and lets tests assert "nothing was
 * sent" without conditional wiring (see ADR 0010).
 */
public enum NotificationChannel {

    /** Standard email delivery (SMTP). */
    EMAIL,

    /** Telegram Bot API messages. */
    TELEGRAM,

    /** No-op channel used as a safe default when no provider is configured. */
    NONE
}
