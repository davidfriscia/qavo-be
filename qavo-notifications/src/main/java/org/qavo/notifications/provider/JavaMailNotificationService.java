/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.notifications.provider;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.UUID;

import org.qavo.core.notifications.NotificationChannel;
import org.qavo.core.notifications.NotificationRequest;
import org.qavo.core.notifications.NotificationResult;
import org.qavo.core.notifications.NotificationService;
import org.qavo.notifications.config.QavoNotificationsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * Email {@link NotificationService} backed by Spring's {@link JavaMailSender}. Renders a plain
 * text body by default and an HTML alternative when {@link
 * NotificationRequest#htmlBody()} is present. Transport-level errors are caught and returned
 * as a {@link NotificationResult#failure(String)} so the calling business operation never has
 * to handle SMTP exceptions directly (this is the fail-soft contract documented on
 * {@code NotificationDispatcher}).
 */
public class JavaMailNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(JavaMailNotificationService.class);

    private final JavaMailSender mailSender;
    private final QavoNotificationsProperties.Email properties;

    public JavaMailNotificationService(JavaMailSender mailSender,
                                       QavoNotificationsProperties.Email properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public NotificationResult send(NotificationRequest request) {
        if (properties.getFrom() == null || properties.getFrom().isBlank()) {
            return NotificationResult.failure(
                    "qavo.notifications.email.from must be configured to send mail");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            boolean multipart = request.htmlBody() != null && !request.htmlBody().isBlank();
            MimeMessageHelper helper = new MimeMessageHelper(message, multipart, "UTF-8");
            helper.setFrom(properties.getFrom());
            helper.setTo(request.recipient());
            if (request.subject() != null) {
                helper.setSubject(request.subject());
            }
            if (multipart) {
                helper.setText(request.body(), request.htmlBody());
            } else {
                helper.setText(request.body(), false);
            }
            mailSender.send(message);
            // The Message-ID header is set by the SMTP layer at send time and we don't have a
            // reliable way to surface it from JavaMail without going through low-level APIs.
            // Emitting a deterministic id keeps the dispatcher's logging contract consistent.
            return NotificationResult.success("smtp-" + UUID.randomUUID());
        } catch (MailException | MessagingException ex) {
            log.warn("Failed to deliver email to {}: {}", request.recipient(), ex.getMessage());
            return NotificationResult.failure(ex.getMessage());
        }
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }
}
