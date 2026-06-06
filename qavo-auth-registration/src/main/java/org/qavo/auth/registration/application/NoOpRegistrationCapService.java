/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.application;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.qavo.core.registration.RegistrationCapService;
import org.qavo.core.registration.RegistrationCapStatus;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Default {@link RegistrationCapService} bean when the cap feature is disabled. Always
 * reports {@code open=true} and ignores recorded registrations. The three Micrometer meters
 * are still registered with constant zero values so dashboards and alerts remain stable when
 * the feature is toggled (architecture §5.7, ADR 0012).
 */
public final class NoOpRegistrationCapService implements RegistrationCapService {

    private final Clock clock;
    private final AtomicInteger zeroGauge = new AtomicInteger(0);

    public NoOpRegistrationCapService(Clock clock, MeterRegistry meterRegistry) {
        this.clock = clock;
        // Register the metric names so consumers can rely on them existing regardless of mode.
        meterRegistry.counter("qavo.registration.cap.check", "result", "allowed");
        meterRegistry.counter("qavo.registration.cap.check", "result", "rejected");
        Gauge.builder("qavo.registration.cap.current_count", zeroGauge, AtomicInteger::get)
                .description("Current count of registrations within the active rolling window")
                .register(meterRegistry);
        Gauge.builder("qavo.registration.cap.utilization", () -> 0.0)
                .description("currentCount / maxRegistrations as a value in [0.0, 1.0]")
                .register(meterRegistry);
    }

    @Override
    public RegistrationCapStatus checkCap() {
        return new RegistrationCapStatus(true, 0, 0, null, null, Instant.now(clock));
    }

    @Override
    public void recordRegistration(String userId) {
        // intentionally empty
    }
}
