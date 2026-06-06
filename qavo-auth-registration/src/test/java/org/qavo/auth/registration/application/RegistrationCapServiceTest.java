/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qavo.auth.registration.config.QavoRegistrationCapProperties;
import org.qavo.auth.registration.domain.RegistrationEvent;
import org.qavo.auth.registration.infrastructure.RegistrationEventRepository;
import org.qavo.core.registration.RegistrationCapStatus;
import org.qavo.security.local.domain.QavoUser;
import org.qavo.security.local.infrastructure.QavoUserRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Focused unit tests for the cap services (ADR 0012). Uses Mockito to stub the repository so the
 * cap arithmetic — including the rolling-window {@code opensAt} computation and the
 * allowed/rejected counter side-effects — can be exercised without standing up a database. The
 * verified-only branch is covered separately so the user-repository fan-out is also asserted.
 */
class RegistrationCapServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

    private RegistrationEventRepository events;
    private QavoUserRepository users;
    private QavoRegistrationCapProperties props;
    private MeterRegistry meterRegistry;
    private Clock clock;

    @BeforeEach
    void setUp() {
        events = mock(RegistrationEventRepository.class);
        users = mock(QavoUserRepository.class);
        props = new QavoRegistrationCapProperties();
        props.setEnabled(true);
        props.setMaxRegistrations(3);
        props.setWindow(Duration.ofHours(24));
        props.setIncludeUnverified(true);
        meterRegistry = new SimpleMeterRegistry();
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Test
    void checkCapReportsOpenWhenCountIsBelowMaximum() {
        when(events.countByRegisteredAtAfter(any())).thenReturn(2L);

        DatabaseRegistrationCapService svc =
                new DatabaseRegistrationCapService(events, users, props, clock, meterRegistry);

        RegistrationCapStatus status = svc.checkCap();
        assertThat(status.open()).isTrue();
        assertThat(status.currentCount()).isEqualTo(2);
        assertThat(status.maxRegistrations()).isEqualTo(3);
        assertThat(status.windowDuration()).isEqualTo(Duration.ofHours(24));
        assertThat(status.opensAt()).isNull();
        assertThat(status.checkedAt()).isEqualTo(NOW);
        assertThat(meterRegistry.counter("qavo.registration.cap.check", "result", "allowed").count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.counter("qavo.registration.cap.check", "result", "rejected").count())
                .isEqualTo(0.0);
    }

    @Test
    void checkCapReportsClosedAndComputesOpensAtWhenAtCapacity() {
        // count == max → closed
        when(events.countByRegisteredAtAfter(any())).thenReturn(3L);
        // The oldest event in the window — opensAt = oldest.registeredAt + window
        Instant oldest = NOW.minus(Duration.ofHours(20));
        when(events.findFirstByRegisteredAtAfterOrderByRegisteredAtAsc(any()))
                .thenReturn(Optional.of(new RegistrationEvent(UUID.randomUUID().toString(), oldest)));

        DatabaseRegistrationCapService svc =
                new DatabaseRegistrationCapService(events, users, props, clock, meterRegistry);

        RegistrationCapStatus status = svc.checkCap();
        assertThat(status.open()).isFalse();
        assertThat(status.currentCount()).isEqualTo(3);
        assertThat(status.opensAt()).isEqualTo(oldest.plus(Duration.ofHours(24)));
        assertThat(meterRegistry.counter("qavo.registration.cap.check", "result", "rejected").count())
                .isEqualTo(1.0);
    }

    @Test
    void checkCapReportsClosedWhenAboveCapacity() {
        when(events.countByRegisteredAtAfter(any())).thenReturn(10L);
        Instant oldest = NOW.minus(Duration.ofHours(1));
        when(events.findFirstByRegisteredAtAfterOrderByRegisteredAtAsc(any()))
                .thenReturn(Optional.of(new RegistrationEvent(UUID.randomUUID().toString(), oldest)));

        DatabaseRegistrationCapService svc =
                new DatabaseRegistrationCapService(events, users, props, clock, meterRegistry);

        RegistrationCapStatus status = svc.checkCap();
        assertThat(status.open()).isFalse();
        assertThat(status.currentCount()).isEqualTo(10);
        assertThat(status.opensAt()).isEqualTo(oldest.plus(Duration.ofHours(24)));
    }

    @Test
    void verifiedOnlyModeExcludesUnverifiedUsersFromTheCount() {
        props.setIncludeUnverified(false);
        UUID verifiedId = UUID.randomUUID();
        UUID unverifiedId = UUID.randomUUID();
        when(events.findUserIdsInWindow(any()))
                .thenReturn(List.of(verifiedId.toString(), unverifiedId.toString()));
        QavoUser verified = new QavoUser(verifiedId, "v", "v@example.com", "h");
        verified.setEmailVerified(true);
        QavoUser unverified = new QavoUser(unverifiedId, "u", "u@example.com", "h");
        unverified.setEmailVerified(false);
        when(users.findById(verifiedId)).thenReturn(Optional.of(verified));
        when(users.findById(unverifiedId)).thenReturn(Optional.of(unverified));

        DatabaseRegistrationCapService svc =
                new DatabaseRegistrationCapService(events, users, props, clock, meterRegistry);

        RegistrationCapStatus status = svc.checkCap();
        // Only the verified user counts → count=1, still under the max of 3, still open.
        assertThat(status.currentCount()).isEqualTo(1);
        assertThat(status.open()).isTrue();
    }

    @Test
    void noOpServiceAlwaysReportsOpenAndExposesAllMetrics() {
        NoOpRegistrationCapService noop = new NoOpRegistrationCapService(clock, meterRegistry);

        RegistrationCapStatus status = noop.checkCap();
        assertThat(status.open()).isTrue();
        assertThat(status.currentCount()).isZero();

        // recordRegistration is a no-op and must not throw.
        noop.recordRegistration("anything");

        // The metric names must still be registered so dashboards stay stable across toggles.
        assertThat(meterRegistry.find("qavo.registration.cap.check").counters()).hasSize(2);
        assertThat(meterRegistry.find("qavo.registration.cap.current_count").gauge()).isNotNull();
        assertThat(meterRegistry.find("qavo.registration.cap.utilization").gauge()).isNotNull();
    }
}
