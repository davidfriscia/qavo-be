/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.qavo.auth.registration.config.QavoRegistrationCapProperties;
import org.qavo.auth.registration.domain.RegistrationEvent;
import org.qavo.auth.registration.infrastructure.RegistrationEventRepository;
import org.qavo.core.registration.RegistrationCapService;
import org.qavo.core.registration.RegistrationCapStatus;
import org.qavo.security.local.domain.QavoUser;
import org.qavo.security.local.infrastructure.QavoUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Database-backed implementation of {@link RegistrationCapService}. Counts rows in
 * {@code qavo_registration_events} whose {@code registered_at} falls inside the current
 * rolling window, and persists a new row per successful registration (architecture §6,
 * ADR 0012).
 *
 * <p>Concurrency: the check-then-record sequence is intentionally NOT protected by a
 * distributed lock — the cap is a soft capacity guard, not a strict quota. Under concurrent
 * load the configured maximum may transiently be exceeded by a small margin (one extra
 * registration per concurrent in-flight check). This is the documented design trade-off; see
 * ADR 0012 §"Soft cap rationale".
 *
 * <p>Transactions: {@link #checkCap()} runs read-only; {@link #recordRegistration(String)}
 * runs in {@link Propagation#REQUIRES_NEW} so the audit row commits independently of the
 * outer registration transaction.
 */
public class DatabaseRegistrationCapService implements RegistrationCapService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseRegistrationCapService.class);

    private final RegistrationEventRepository events;
    private final QavoUserRepository users;
    private final QavoRegistrationCapProperties properties;
    private final Clock clock;
    private final AtomicInteger currentCountGauge = new AtomicInteger(0);
    private final Counter allowedCounter;
    private final Counter rejectedCounter;

    public DatabaseRegistrationCapService(RegistrationEventRepository events,
                                          QavoUserRepository users,
                                          QavoRegistrationCapProperties properties,
                                          Clock clock,
                                          MeterRegistry meterRegistry) {
        this.events = events;
        this.users = users;
        this.properties = properties;
        this.clock = clock;
        this.allowedCounter = Counter.builder("qavo.registration.cap.check")
                .description("Number of registration-cap check calls by outcome")
                .tag("result", "allowed").register(meterRegistry);
        this.rejectedCounter = Counter.builder("qavo.registration.cap.check")
                .description("Number of registration-cap check calls by outcome")
                .tag("result", "rejected").register(meterRegistry);
        Gauge.builder("qavo.registration.cap.current_count", currentCountGauge, AtomicInteger::get)
                .description("Current count of registrations within the active rolling window")
                .register(meterRegistry);
        Gauge.builder("qavo.registration.cap.utilization", this, DatabaseRegistrationCapService::utilization)
                .description("currentCount / maxRegistrations as a value in [0.0, 1.0]")
                .register(meterRegistry);
    }

    @Override
    @Transactional(readOnly = true)
    public RegistrationCapStatus checkCap() {
        Instant now = clock.instant();
        Duration window = properties.getWindow();
        int max = properties.getMaxRegistrations() != null ? properties.getMaxRegistrations() : 0;
        Instant windowStart = now.minus(window);
        int count = (int) Math.min(Integer.MAX_VALUE, currentWindowCount(windowStart));
        currentCountGauge.set(count);
        boolean open = count < max;
        Instant opensAt = open ? null : computeOpensAt(windowStart, window);
        RegistrationCapStatus status = new RegistrationCapStatus(
                open, count, max, window, opensAt, now);
        if (open) {
            allowedCounter.increment();
        } else {
            rejectedCounter.increment();
        }
        return status;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRegistration(String userId) {
        // qavo-design: REQUIRES_NEW so the audit row commits independently of any outer
        // transaction. The registration use case calls this *after* the user row is saved;
        // even if the outer flow later rolls back, the event is preserved — the cap counts
        // attempts that succeeded as far as user creation.
        events.save(new RegistrationEvent(userId, clock.instant()));
    }

    private long currentWindowCount(Instant windowStart) {
        if (properties.isIncludeUnverified()) {
            return events.countByRegisteredAtAfter(windowStart);
        }
        // Verified-only mode: fetch user ids in window, then ask the user repository how many
        // of them are verified. Two queries instead of a cross-entity JPQL join — see the
        // qavo-design comment on RegistrationEventRepository.
        List<String> userIds = events.findUserIdsInWindow(windowStart);
        if (userIds.isEmpty()) {
            return 0L;
        }
        return userIds.stream()
                .map(this::tryParseUuid)
                .flatMap(Optional::stream)
                .map(users::findById)
                .flatMap(Optional::stream)
                .filter(QavoUser::isEmailVerified)
                .count();
    }

    private Instant computeOpensAt(Instant windowStart, Duration window) {
        if (properties.isIncludeUnverified()) {
            return events.findFirstByRegisteredAtAfterOrderByRegisteredAtAsc(windowStart)
                    .map(e -> e.getRegisteredAt().plus(window))
                    .orElse(null);
        }
        // Verified-only mode: walk events in window ascending and return the oldest whose
        // owner is verified. List size is bounded by maxRegistrations in practice.
        List<RegistrationEvent> inWindow = events.findEventsInWindow(
                windowStart, PageRequest.of(0, Math.max(1, properties.getMaxRegistrations())));
        return inWindow.stream()
                .filter(e -> tryParseUuid(e.getUserId())
                        .flatMap(users::findById)
                        .map(QavoUser::isEmailVerified)
                        .orElse(false))
                .map(e -> e.getRegisteredAt().plus(window))
                .findFirst()
                .orElse(null);
    }

    private Optional<UUID> tryParseUuid(String raw) {
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            log.warn("Skipping registration event with non-UUID userId={} (cap count may be off by one)", raw);
            return Optional.empty();
        }
    }

    private double utilization() {
        int max = properties.getMaxRegistrations() != null ? properties.getMaxRegistrations() : 0;
        if (max <= 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) currentCountGauge.get() / (double) max);
    }
}
