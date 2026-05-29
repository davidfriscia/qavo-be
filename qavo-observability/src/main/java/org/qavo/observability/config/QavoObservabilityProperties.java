/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.observability.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the observability pipeline under {@code qavo.observability.*}
 * (see architecture &sect;5.3).
 */
@ConfigurationProperties(prefix = "qavo.observability")
public class QavoObservabilityProperties {

    /**
     * Logical application name stamped onto every log entry as the {@code appName} MDC field.
     * Defaults to the Spring application name when left unset.
     */
    private String applicationName = "qavo-app";

    /**
     * Whether to generate a {@code traceId} when an inbound request carries no W3C
     * {@code traceparent} header. Enforced on by default — every request must be traceable.
     */
    private boolean generateTraceIdIfMissing = true;

    /** Response header used to echo the resolved {@code traceId} back to the client. */
    private String responseTraceHeader = "X-Trace-Id";

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public boolean isGenerateTraceIdIfMissing() {
        return generateTraceIdIfMissing;
    }

    public void setGenerateTraceIdIfMissing(boolean generateTraceIdIfMissing) {
        this.generateTraceIdIfMissing = generateTraceIdIfMissing;
    }

    public String getResponseTraceHeader() {
        return responseTraceHeader;
    }

    public void setResponseTraceHeader(String responseTraceHeader) {
        this.responseTraceHeader = responseTraceHeader;
    }
}
