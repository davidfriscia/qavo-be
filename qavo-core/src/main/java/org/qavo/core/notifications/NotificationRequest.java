/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.notifications;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable, provider-agnostic description of a notification to dispatch (architecture
 * &sect;5 cross-cutting infrastructure). The same record is accepted by every
 * {@link NotificationService} implementation; channel-specific fields are interpreted by the
 * service that supports the {@link NotificationChannel}, and ignored by services that do not.
 *
 * <p>The {@code subject} field is meaningful for channels with a subject concept (email);
 * other channels (Telegram) ignore it. The {@code htmlBody} field is optional rich content,
 * always paired with a plain-text {@code body} as the canonical representation. {@code
 * metadata} carries arbitrary provider hints (precedence tags, reply-to overrides, &hellip;)
 * and is never {@code null} — callers receive an immutable empty map instead.
 */
public record NotificationRequest(
        NotificationChannel channel,
        String recipient,
        String subject,
        String body,
        String htmlBody,
        Map<String, Object> metadata) {

    /**
     * Compact canonical constructor. Validates the non-null invariants that every channel
     * relies on and copies the {@code metadata} map so the record stays effectively
     * immutable regardless of what the caller does with the original reference.
     */
    public NotificationRequest {
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(recipient, "recipient must not be null");
        if (recipient.isBlank()) {
            throw new IllegalArgumentException("recipient must not be blank");
        }
        Objects.requireNonNull(body, "body must not be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /** Convenience factory for the common email case (no rich body, no metadata). */
    public static NotificationRequest email(String recipient, String subject, String body) {
        return new NotificationRequest(NotificationChannel.EMAIL, recipient, subject, body, null, Map.of());
    }

    /** Convenience factory for an email with an HTML alternative. */
    public static NotificationRequest email(String recipient, String subject, String body, String htmlBody) {
        return new NotificationRequest(NotificationChannel.EMAIL, recipient, subject, body, htmlBody, Map.of());
    }

    /**
     * Convenience factory for Telegram. The {@code recipient} carries the Telegram
     * {@code chat_id}; the platform does not impose a numeric/string distinction so usernames
     * (prefixed with {@code @}) work the same way the Telegram API does.
     */
    public static NotificationRequest telegram(String chatId, String body) {
        return new NotificationRequest(NotificationChannel.TELEGRAM, chatId, null, body, null, Map.of());
    }
}
