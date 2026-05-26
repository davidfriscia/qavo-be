/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.plugin;

/**
 * Service Provider Interface implemented by every optional Qavo capability plugin
 * (see architecture &sect;6).
 *
 * <p>A plugin auto-configures itself through Spring Boot's conditional mechanisms and publishes
 * exactly one {@code QavoPlugin} bean describing itself. The {@link PluginRegistry} collects
 * every such bean, giving the platform a runtime inventory of which capabilities are present.
 * Because presence is determined by what the application chose to import, removing a plugin
 * dependency removes its descriptor — and therefore the capability — entirely.
 */
public interface QavoPlugin {

    /** Stable, kebab-case identifier (e.g. {@code auth-login}). Unique across plugins. */
    String id();

    /** Human-readable display name (e.g. {@code Local Login}). */
    String name();

    /** Plugin artifact version, surfaced in diagnostics and the plugin inventory. */
    String version();

    /** Short description of the capability the plugin contributes. */
    default String description() {
        return "";
    }
}
