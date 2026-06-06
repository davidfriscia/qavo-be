/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.notifications.provider;

import org.qavo.core.observability.TraceContext;
import org.slf4j.MDC;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Minimal {@link TraceContext} stub exposing MDC's {@code traceId}. Required by the
 * {@code QavoHttpClientAutoConfiguration} to populate the trace header on outbound calls
 * from the Telegram provider tests. Kept local to the notifications module so the test
 * slice stays self-contained.
 */
@TestConfiguration
class TestTraceContextConfig {
    @Bean
    TraceContext traceContext() {
        return new TraceContext() {
            @Override public String currentTraceId() { return MDC.get("traceId"); }
            @Override public String currentSpanId() { return null; }
        };
    }
}
