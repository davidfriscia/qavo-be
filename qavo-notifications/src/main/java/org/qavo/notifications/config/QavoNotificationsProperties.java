/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.notifications.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Root configuration bound to {@code qavo.notifications.*}. The top-level structure mirrors
 * the {@link org.qavo.core.notifications.NotificationChannel} enum so each channel has its own
 * properly-typed configuration block; this keeps the property surface stable while letting
 * future channels be added without rebinding existing keys.
 */
@ConfigurationProperties(prefix = "qavo.notifications")
public class QavoNotificationsProperties {

    /**
     * Master switch for the notifications plugin. When {@code false}, the dispatcher and no
     * provider beans are wired and any call site that depends on a {@code NotificationDispatcher}
     * sees no bean — letting application code decide whether to require its presence.
     */
    private boolean enabled = true;

    @NestedConfigurationProperty
    private final Email email = new Email();

    @NestedConfigurationProperty
    private final Telegram telegram = new Telegram();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Email getEmail() {
        return email;
    }

    public Telegram getTelegram() {
        return telegram;
    }

    /** Configuration block bound to {@code qavo.notifications.email.*}. */
    public static class Email {
        /**
         * Activates the JavaMail-backed provider. When {@code false}, the email channel falls
         * through to the no-op provider so unconfigured environments do not attempt SMTP.
         */
        private boolean enabled = false;

        /**
         * Default {@code From} address used when a {@link
         * org.qavo.core.notifications.NotificationRequest} does not specify a sender via
         * metadata. Required when {@code enabled=true}.
         */
        private String from;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }
    }

    /** Configuration block bound to {@code qavo.notifications.telegram.*}. */
    public static class Telegram {
        /**
         * Activates the Telegram-backed provider. When {@code false}, the Telegram channel
         * falls through to the no-op provider.
         */
        private boolean enabled = false;

        /**
         * Bot token issued by BotFather. Required when {@code enabled=true}. The provider
         * sends messages by POSTing to {@code https://api.telegram.org/bot{token}/sendMessage}.
         */
        private String botToken;

        /**
         * Name of the {@link org.qavo.resilience.http.QavoHttpClient} the provider should use
         * to talk to the Telegram Bot API. The platform standardizes the name to {@code
         * "telegram"}; consuming applications declare the matching entry under
         * {@code qavo.resilience.http.clients}.
         */
        private String clientName = "telegram";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBotToken() {
            return botToken;
        }

        public void setBotToken(String botToken) {
            this.botToken = botToken;
        }

        public String getClientName() {
            return clientName;
        }

        public void setClientName(String clientName) {
            this.clientName = clientName;
        }
    }
}
