/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.notifications.dispatch;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import java.util.List;
import java.util.Objects;

import org.qavo.core.notifications.NotificationChannel;
import org.qavo.core.notifications.NotificationDispatcher;
import org.qavo.core.notifications.NotificationRequest;
import org.qavo.core.notifications.NotificationResult;
import org.qavo.core.notifications.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link NotificationDispatcher} that delegates each request to the first
 * {@link NotificationService} reporting support for the request's channel. Providers are
 * injected as an ordered list; specific providers (e.g. JavaMail for EMAIL, Telegram for
 * TELEGRAM) are placed ahead of the fail-soft {@code NoOpNotificationService} by the
 * autoconfiguration so configured channels always win precedence.
 *
 * <p>Two metric instruments are published when a {@link MeterRegistry} is available:
 * <ul>
 *   <li>{@code qavo.notifications.sent} — counter tagged with {@code channel} and
 *       {@code status} (success/failure), incremented once per dispatch.</li>
 *   <li>{@code qavo.notifications.providers.registered} — gauge over the size of the provider
 *       list, useful to confirm at startup that the expected channels were wired.</li>
 * </ul>
 * When Micrometer is absent (the {@code MeterRegistry} dependency is optional in this module),
 * dispatch still functions; only metric emission is skipped.
 */
public class DefaultNotificationDispatcher implements NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationDispatcher.class);

    private final List<NotificationService> providers;
    private final MeterRegistry meterRegistry;

    public DefaultNotificationDispatcher(List<NotificationService> providers,
                                         MeterRegistry meterRegistry) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
        this.meterRegistry = meterRegistry;
        if (meterRegistry != null) {
            meterRegistry.gauge(
                    "qavo.notifications.providers.registered",
                    Tags.empty(),
                    this.providers,
                    List::size);
        }
    }

    @Override
    public NotificationResult dispatch(NotificationRequest request) {
        Objects.requireNonNull(request, "request");
        NotificationChannel channel = request.channel();
        for (NotificationService provider : providers) {
            if (provider.supports(channel)) {
                NotificationResult result = provider.send(request);
                recordMetric(channel, result.success());
                if (!result.success()) {
                    log.warn("Notification dispatch failed channel={} recipient={} error={}",
                            channel, request.recipient(), result.errorMessage());
                }
                return result;
            }
        }
        recordMetric(channel, false);
        return NotificationResult.failure(
                "No NotificationService configured for channel " + channel);
    }

    private void recordMetric(NotificationChannel channel, boolean success) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter(
                "qavo.notifications.sent",
                "channel", channel.name(),
                "status", success ? "success" : "failure")
                .increment();
    }
}
