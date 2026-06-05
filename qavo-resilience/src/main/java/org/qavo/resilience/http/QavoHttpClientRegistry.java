/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.resilience.http;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Lookup over the {@link QavoHttpClient} instances materialized from
 * {@code qavo.resilience.http.clients}. A registry (rather than a bag of named beans) keeps the
 * dependency from the application code to the resilience module explicit: callers depend on the
 * registry interface and resolve their own backend by name, mirroring how Resilience4j's own
 * registries work.
 */
public class QavoHttpClientRegistry {

    private final Map<String, QavoHttpClient> clientsByName;

    public QavoHttpClientRegistry(Map<String, QavoHttpClient> clientsByName) {
        this.clientsByName = Map.copyOf(clientsByName);
    }

    /** Returns the client configured under {@code name}, or throws if none was declared. */
    public QavoHttpClient get(String name) {
        QavoHttpClient client = clientsByName.get(name);
        if (client == null) {
            throw new NoSuchElementException(
                    "No QavoHttpClient configured under name '%s'. Declared clients: %s"
                            .formatted(name, clientsByName.keySet()));
        }
        return client;
    }

    /** Live view of the configured client names; primarily for diagnostics. */
    public java.util.Set<String> names() {
        return clientsByName.keySet();
    }
}
