/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.resilience.http;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound to {@code qavo.resilience.http.*}. Declares one entry per logical outbound backend; the
 * autoconfiguration materializes a {@link QavoHttpClient} bean for each. Keeping the structure
 * explicit (rather than auto-discovering Resilience4j instances) avoids creating clients with no
 * base URL and makes the dependency from application configuration to runtime topology obvious.
 *
 * <pre>{@code
 * qavo:
 *   resilience:
 *     http:
 *       clients:
 *         catalog:
 *           base-url: https://catalog.internal.example
 *           connect-timeout: PT2S
 *           read-timeout: PT5S
 * }</pre>
 */
@ConfigurationProperties(prefix = "qavo.resilience.http")
public class QavoHttpClientProperties {

    /** Header name used to forward the current {@code traceId}; standardized as {@code X-Trace-Id}. */
    private String traceHeader = "X-Trace-Id";

    /** Map of client name → configuration. The key becomes {@link QavoHttpClient#name()}. */
    private Map<String, Client> clients = new LinkedHashMap<>();

    public String getTraceHeader() {
        return traceHeader;
    }

    public void setTraceHeader(String traceHeader) {
        this.traceHeader = traceHeader;
    }

    public Map<String, Client> getClients() {
        return clients;
    }

    public void setClients(Map<String, Client> clients) {
        this.clients = clients;
    }

    /** Per-client configuration block. */
    public static class Client {
        private String baseUrl;
        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration readTimeout = Duration.ofSeconds(10);

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }
    }
}
