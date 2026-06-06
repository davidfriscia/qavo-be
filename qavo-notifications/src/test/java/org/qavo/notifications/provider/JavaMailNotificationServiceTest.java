/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.notifications.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;

import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.qavo.core.notifications.NotificationChannel;
import org.qavo.core.notifications.NotificationRequest;
import org.qavo.core.notifications.NotificationResult;
import org.qavo.notifications.config.QavoNotificationsProperties;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * End-to-end test of {@link JavaMailNotificationService}: drives the provider against a
 * GreenMail in-memory SMTP server, asserts that the message reaches the inbox with the
 * expected headers and body, and verifies the {@code supports(...)} contract.
 */
class JavaMailNotificationServiceTest {

    @RegisterExtension
    static final GreenMailExtension GREEN = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig()
                    .withUser("qavo", "qavo"));

    private JavaMailSenderImpl mailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(GREEN.getSmtp().getBindTo());
        sender.setPort(GREEN.getSmtp().getPort());
        sender.setUsername("qavo");
        sender.setPassword("qavo");
        sender.getJavaMailProperties().setProperty("mail.smtp.auth", "true");
        sender.getJavaMailProperties().setProperty("mail.smtp.starttls.enable", "false");
        return sender;
    }

    private QavoNotificationsProperties.Email props() {
        QavoNotificationsProperties.Email props = new QavoNotificationsProperties.Email();
        props.setEnabled(true);
        props.setFrom("noreply@qavo.test");
        return props;
    }

    @Test
    void delivers_plain_text_email_to_recipient() throws Exception {
        JavaMailNotificationService service = new JavaMailNotificationService(mailSender(), props());

        NotificationResult result = service.send(
                NotificationRequest.email("user@qavo.test", "Verify your email", "Open this link"));

        assertThat(result.success()).isTrue();
        assertThat(result.providerMessageId()).startsWith("smtp-");
        MimeMessage[] received = GREEN.getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getSubject()).isEqualTo("Verify your email");
        assertThat(received[0].getAllRecipients()[0].toString()).isEqualTo("user@qavo.test");
        assertThat(received[0].getContent().toString()).contains("Open this link");
    }

    @Test
    void fails_softly_when_from_address_missing() {
        QavoNotificationsProperties.Email props = new QavoNotificationsProperties.Email();
        props.setEnabled(true);
        // from intentionally left null
        JavaMailNotificationService service = new JavaMailNotificationService(mailSender(), props);

        NotificationResult result = service.send(
                NotificationRequest.email("user@qavo.test", "Hi", "body"));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("qavo.notifications.email.from");
    }

    @Test
    void supports_only_email_channel() {
        JavaMailNotificationService service = new JavaMailNotificationService(mailSender(), props());

        assertThat(service.supports(NotificationChannel.EMAIL)).isTrue();
        assertThat(service.supports(NotificationChannel.TELEGRAM)).isFalse();
        assertThat(service.supports(NotificationChannel.NONE)).isFalse();
    }
}
