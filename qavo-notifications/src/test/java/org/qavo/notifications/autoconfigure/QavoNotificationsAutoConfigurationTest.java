/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.notifications.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.qavo.core.notifications.NotificationDispatcher;
import org.qavo.notifications.provider.JavaMailNotificationService;
import org.qavo.notifications.provider.NoOpNotificationService;
import org.qavo.notifications.provider.TelegramNotificationService;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderValidatorAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Auto-configuration slice tests for {@link QavoNotificationsAutoConfiguration}: verifies that
 * the dispatcher is always wired, that the JavaMail provider only appears when the email
 * sender bean exists AND {@code qavo.notifications.email.enabled=true}, and that the no-op
 * provider stays present in every shape so dispatch is always safe.
 */
class QavoNotificationsAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(QavoNotificationsAutoConfiguration.class));

    @Test
    void dispatcher_and_noop_always_present_when_plugin_enabled() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(NotificationDispatcher.class);
            assertThat(context).hasSingleBean(NoOpNotificationService.class);
            assertThat(context).doesNotHaveBean(JavaMailNotificationService.class);
            assertThat(context).doesNotHaveBean(TelegramNotificationService.class);
        });
    }

    @Test
    void no_beans_when_plugin_disabled() {
        runner.withPropertyValues("qavo.notifications.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(NotificationDispatcher.class);
                    assertThat(context).doesNotHaveBean(NoOpNotificationService.class);
                });
    }

    @Test
    void javamail_provider_wired_when_mail_sender_present_and_email_enabled() {
        runner.withConfiguration(AutoConfigurations.of(
                        MailSenderAutoConfiguration.class,
                        MailSenderValidatorAutoConfiguration.class))
                .withPropertyValues(
                        "qavo.notifications.email.enabled=true",
                        "qavo.notifications.email.from=test@qavo.io",
                        "spring.mail.host=localhost",
                        "spring.mail.port=2525")
                .run(context -> {
                    assertThat(context).hasSingleBean(JavaMailNotificationService.class);
                    assertThat(context).hasSingleBean(NoOpNotificationService.class);
                });
    }

    @Test
    void javamail_provider_skipped_when_email_disabled() {
        runner.withConfiguration(AutoConfigurations.of(
                        MailSenderAutoConfiguration.class,
                        MailSenderValidatorAutoConfiguration.class))
                .withPropertyValues("spring.mail.host=localhost")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(JavaMailNotificationService.class);
                });
    }
}
