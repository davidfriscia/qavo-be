/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.notifications.provider;

import java.util.Map;

import org.qavo.core.notifications.NotificationChannel;
import org.qavo.core.notifications.NotificationRequest;
import org.qavo.core.notifications.NotificationResult;
import org.qavo.core.notifications.NotificationService;
import org.qavo.notifications.config.QavoNotificationsProperties;
import org.qavo.resilience.http.QavoHttpClient;
import org.qavo.resilience.http.QavoHttpClientRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

/**
 * Telegram {@link NotificationService} that sends messages by POSTing to {@code
 * https://api.telegram.org/bot{token}/sendMessage} via the platform's standardized {@link
 * QavoHttpClient} (so retries, circuit breakers, and trace propagation are applied). The
 * client is resolved by name from the {@link QavoHttpClientRegistry}; by default the platform
 * uses the name {@code "telegram"} but consuming applications can override it via {@code
 * qavo.notifications.telegram.client-name}.
 *
 * <p>Network and provider failures are caught and returned as a {@link
 * NotificationResult#failure(String)} per the fail-soft dispatcher contract.
 */
public class TelegramNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final QavoHttpClientRegistry clientRegistry;
    private final QavoNotificationsProperties.Telegram properties;

    public TelegramNotificationService(QavoHttpClientRegistry clientRegistry,
                                       QavoNotificationsProperties.Telegram properties) {
        this.clientRegistry = clientRegistry;
        this.properties = properties;
    }

    @Override
    public NotificationResult send(NotificationRequest request) {
        if (properties.getBotToken() == null || properties.getBotToken().isBlank()) {
            return NotificationResult.failure(
                    "qavo.notifications.telegram.bot-token must be configured");
        }
        try {
            QavoHttpClient client = clientRegistry.get(properties.getClientName());
            String path = "/bot" + properties.getBotToken() + "/sendMessage";
            Map<String, Object> payload = Map.of(
                    "chat_id", request.recipient(),
                    "text", request.body());
            ResponseEntity<Map> response = client.post(path, payload, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                Object result = response.getBody() == null ? null : response.getBody().get("result");
                String messageId = null;
                if (result instanceof Map<?, ?> resultMap) {
                    Object id = resultMap.get("message_id");
                    if (id != null) {
                        messageId = id.toString();
                    }
                }
                return NotificationResult.success(messageId);
            }
            return NotificationResult.failure(
                    "Telegram API responded with HTTP " + response.getStatusCode().value());
        } catch (RuntimeException ex) {
            log.warn("Failed to deliver Telegram message to {}: {}",
                    request.recipient(), ex.getMessage());
            return NotificationResult.failure(ex.getMessage());
        }
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.TELEGRAM;
    }
}
