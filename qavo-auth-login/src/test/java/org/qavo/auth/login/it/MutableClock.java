/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.it;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link Clock} whose instant can be advanced from tests, used to simulate the passage of time
 * (e.g. refresh-token expiry) deterministically without {@code Thread.sleep}. Replaces the default
 * system clock in the local login plugin through a {@code @TestConfiguration} override.
 */
public final class MutableClock extends Clock {

    private final AtomicReference<Instant> now;
    private final ZoneId zone;

    public MutableClock(Instant initial) {
        this(initial, ZoneOffset.UTC);
    }

    private MutableClock(Instant initial, ZoneId zone) {
        this.now = new AtomicReference<>(initial);
        this.zone = zone;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        return new MutableClock(now.get(), newZone);
    }

    @Override
    public Instant instant() {
        return now.get();
    }

    public void advance(java.time.Duration delta) {
        now.updateAndGet(current -> current.plus(delta));
    }

    public void set(Instant when) {
        now.set(when);
    }
}
