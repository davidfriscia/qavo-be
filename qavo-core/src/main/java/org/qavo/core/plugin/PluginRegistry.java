/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.plugin;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Runtime inventory of the {@link QavoPlugin}s present in the application context.
 *
 * <p>The registry is populated by Spring injecting every {@code QavoPlugin} bean discovered in
 * the context. It is read-only: a plugin's existence is decided at build time by the modules
 * the application imports, not by mutating the registry at runtime.
 */
public class PluginRegistry {

    private final List<QavoPlugin> plugins;

    public PluginRegistry(List<QavoPlugin> plugins) {
        this.plugins = plugins.stream()
                .sorted(Comparator.comparing(QavoPlugin::id))
                .toList();
    }

    /** All registered plugins, ordered by identifier. */
    public List<QavoPlugin> getPlugins() {
        return plugins;
    }

    /** Looks up a plugin by its stable identifier. */
    public Optional<QavoPlugin> findById(String id) {
        return plugins.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    /** Whether a plugin with the given identifier is active. */
    public boolean isPresent(String id) {
        return findById(id).isPresent();
    }

    /** Number of active plugins. */
    public int count() {
        return plugins.size();
    }
}
