/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.notifications.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.qavo.core.notifications.NotificationChannel;
import org.qavo.core.notifications.NotificationRequest;
import org.qavo.core.notifications.NotificationResult;
import org.qavo.core.notifications.NotificationService;
import org.qavo.notifications.provider.NoOpNotificationService;

/**
 * Unit tests for {@link DefaultNotificationDispatcher}: precedence between configured
 * providers and the no-op fallback, fail-soft behavior when no provider supports the channel,
 * and Micrometer metric emission.
 */
class DefaultNotificationDispatcherTest {

    @Test
    void delegates_to_first_supporting_provider() {
        NotificationService email = mock(NotificationService.class);
        when(email.supports(NotificationChannel.EMAIL)).thenReturn(true);
        when(email.send(any())).thenReturn(NotificationResult.success("smtp-1"));

        DefaultNotificationDispatcher dispatcher = new DefaultNotificationDispatcher(
                List.of(email, new NoOpNotificationService()), null);

        NotificationResult result = dispatcher.dispatch(
                NotificationRequest.email("a@b", "Hi", "body"));

        assertThat(result.success()).isTrue();
        assertThat(result.providerMessageId()).isEqualTo("smtp-1");
        verify(email, times(1)).send(any());
    }

    @Test
    void falls_back_to_noop_when_no_provider_supports_channel() {
        NotificationService emailOnly = mock(NotificationService.class);
        when(emailOnly.supports(any())).thenAnswer(inv -> inv.getArgument(0) == NotificationChannel.EMAIL);

        DefaultNotificationDispatcher dispatcher = new DefaultNotificationDispatcher(
                List.of(emailOnly, new NoOpNotificationService()), null);

        NotificationResult result = dispatcher.dispatch(
                NotificationRequest.telegram("12345", "hi"));

        // No-op accepts every channel so it always returns success="noop".
        assertThat(result.success()).isTrue();
        assertThat(result.providerMessageId()).isEqualTo("noop");
        verify(emailOnly, never()).send(any());
    }

    @Test
    void returns_failure_when_no_provider_at_all() {
        DefaultNotificationDispatcher dispatcher = new DefaultNotificationDispatcher(
                List.of(), null);

        NotificationResult result = dispatcher.dispatch(
                NotificationRequest.email("a@b", "Hi", "body"));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("No NotificationService configured");
    }

    @Test
    void emits_success_and_failure_counters_when_meter_registry_present() {
        MeterRegistry registry = new SimpleMeterRegistry();
        NotificationService failing = mock(NotificationService.class);
        when(failing.supports(NotificationChannel.EMAIL)).thenReturn(true);
        when(failing.send(any())).thenReturn(NotificationResult.failure("smtp down"));

        NotificationService ok = mock(NotificationService.class);
        when(ok.supports(NotificationChannel.TELEGRAM)).thenReturn(true);
        when(ok.send(any())).thenReturn(NotificationResult.success("tg-1"));

        DefaultNotificationDispatcher dispatcher = new DefaultNotificationDispatcher(
                List.of(failing, ok), registry);

        dispatcher.dispatch(NotificationRequest.email("a@b", "s", "body"));
        dispatcher.dispatch(NotificationRequest.telegram("1", "hi"));

        assertThat(registry.counter(
                "qavo.notifications.sent", "channel", "EMAIL", "status", "failure").count())
                .isEqualTo(1.0);
        assertThat(registry.counter(
                "qavo.notifications.sent", "channel", "TELEGRAM", "status", "success").count())
                .isEqualTo(1.0);
        assertThat(registry.find("qavo.notifications.providers.registered").gauge().value())
                .isEqualTo(2.0);
    }
}
