/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.observability;

/**
 * Canonical MDC (Mapped Diagnostic Context) keys enforced across the platform.
 *
 * <p>The platform guarantees that {@link #TRACE_ID}, {@link #APP_NAME} and {@link #USER_ID}
 * are present on every log entry (see architecture &sect;5.3). Keys are defined in the core so
 * that every module — logging, error handling, the HTTP client — agrees on the contract.
 */
public final class MdcKeys {

    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String APP_NAME = "appName";
    public static final String USER_ID = "userId";

    private MdcKeys() {
        throw new AssertionError("Constants holder must not be instantiated");
    }
}
