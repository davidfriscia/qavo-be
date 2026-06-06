/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.notifications.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.List;

import org.qavo.core.notifications.NotificationDispatcher;
import org.qavo.core.notifications.NotificationService;
import org.qavo.notifications.config.QavoNotificationsProperties;
import org.qavo.notifications.dispatch.DefaultNotificationDispatcher;
import org.qavo.notifications.provider.JavaMailNotificationService;
import org.qavo.notifications.provider.NoOpNotificationService;
import org.qavo.notifications.provider.TelegramNotificationService;
import org.qavo.resilience.http.QavoHttpClientRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Wires the notifications plugin. Each provider is conditional on its dedicated
 * {@code qavo.notifications.<channel>.enabled} property so applications enable channels
 * declaratively. The {@link NoOpNotificationService} is always present as the lowest-priority
 * fallback (highest {@code @Order}); the {@link DefaultNotificationDispatcher} then receives
 * every {@link NotificationService} bean in priority order and routes by channel.
 *
 * <p>Ordered with {@code after = MailSenderAutoConfiguration.class} so the
 * {@link JavaMailSender} bean is registered before our {@code @ConditionalOnBean} runs;
 * without the explicit ordering Spring may evaluate our condition first and silently skip
 * the email provider (the same trap that bit qavo-auth-login pre-0.0.2).
 */
@AutoConfiguration(after = MailSenderAutoConfiguration.class)
@ConditionalOnProperty(prefix = "qavo.notifications", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(QavoNotificationsProperties.class)
public class QavoNotificationsAutoConfiguration {

    public static final String PLUGIN_ID = "notifications";
    public static final String PLUGIN_VERSION = "0.0.3-SNAPSHOT";

    /**
     * Email provider. Activated only when both the JavaMail sender bean exists (the consuming
     * application included {@code spring-boot-starter-mail} and configured {@code spring.mail.*})
     * and {@code qavo.notifications.email.enabled=true}.
     */
    @Bean
    @Order(10)
    @ConditionalOnClass(JavaMailSender.class)
    @ConditionalOnBean(JavaMailSender.class)
    @ConditionalOnProperty(prefix = "qavo.notifications.email", name = "enabled",
            havingValue = "true")
    @ConditionalOnMissingBean(name = "qavoJavaMailNotificationService")
    public JavaMailNotificationService qavoJavaMailNotificationService(
            JavaMailSender mailSender,
            QavoNotificationsProperties properties) {
        return new JavaMailNotificationService(mailSender, properties.getEmail());
    }

    /**
     * Telegram provider. Activated when {@code qavo.notifications.telegram.enabled=true} and
     * a {@link QavoHttpClientRegistry} is available; the bot token and target client name come
     * from {@link QavoNotificationsProperties.Telegram}.
     */
    @Bean
    @Order(10)
    @ConditionalOnClass(QavoHttpClientRegistry.class)
    @ConditionalOnBean(QavoHttpClientRegistry.class)
    @ConditionalOnProperty(prefix = "qavo.notifications.telegram", name = "enabled",
            havingValue = "true")
    @ConditionalOnMissingBean(name = "qavoTelegramNotificationService")
    public TelegramNotificationService qavoTelegramNotificationService(
            QavoHttpClientRegistry registry,
            QavoNotificationsProperties properties) {
        return new TelegramNotificationService(registry, properties.getTelegram());
    }

    /**
     * No-op fallback. Always present at the lowest priority so the dispatcher can always
     * produce a result and the omission of a real provider is observable via the warn log
     * line inside {@link NoOpNotificationService} rather than a runtime crash.
     */
    @Bean
    @Order(Integer.MAX_VALUE)
    @ConditionalOnMissingBean(NoOpNotificationService.class)
    public NoOpNotificationService qavoNoOpNotificationService() {
        return new NoOpNotificationService();
    }

    /**
     * Dispatcher facade. Receives the ordered provider list (Spring honors {@code @Order} on
     * the bean methods) and an optional {@link MeterRegistry} for emitting dispatch counters.
     */
    @Bean
    @ConditionalOnMissingBean
    public NotificationDispatcher qavoNotificationDispatcher(
            List<NotificationService> providers,
            ObjectProvider<MeterRegistry> meterRegistry) {
        return new DefaultNotificationDispatcher(providers, meterRegistry.getIfAvailable());
    }
}
