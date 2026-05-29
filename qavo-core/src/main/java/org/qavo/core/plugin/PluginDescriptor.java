/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.plugin;

/**
 * Immutable, ready-to-use {@link QavoPlugin} implementation. Plugins that need nothing more
 * than static metadata can publish a {@code PluginDescriptor} bean directly instead of writing
 * a bespoke class.
 *
 * @param id          stable kebab-case identifier
 * @param name        human-readable name
 * @param version     artifact version
 * @param description short capability description
 */
public record PluginDescriptor(String id, String name, String version, String description)
        implements QavoPlugin {

    public static PluginDescriptor of(String id, String name, String version) {
        return new PluginDescriptor(id, name, version, "");
    }
}
