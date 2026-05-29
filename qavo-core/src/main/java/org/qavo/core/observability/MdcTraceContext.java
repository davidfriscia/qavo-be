/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.observability;

import org.slf4j.MDC;

/**
 * Default {@link TraceContext} that reads the correlation identifiers from SLF4J's MDC,
 * which the observability module populates on every inbound request (see architecture &sect;5.3).
 */
public class MdcTraceContext implements TraceContext {

    @Override
    public String currentTraceId() {
        return MDC.get(MdcKeys.TRACE_ID);
    }

    @Override
    public String currentSpanId() {
        return MDC.get(MdcKeys.SPAN_ID);
    }
}
