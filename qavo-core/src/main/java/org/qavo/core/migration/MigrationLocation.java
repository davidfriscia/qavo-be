/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.migration;

/**
 * Declares a classpath location that a module (core or plugin) contributes to the aggregated
 * Flyway migration set (see architecture &sect;6 and &sect;8).
 *
 * <p>Each module publishes one {@code MigrationLocation} bean and ships its SQL under a reserved
 * folder (e.g. {@code classpath:db/qavo/security}). The platform discovers every such
 * bean and merges the locations, so plugin migrations run automatically and leave with the plugin
 * when it is removed. Modules also prefix their version numbers to avoid collisions across the
 * shared history table.
 *
 * @param location classpath location in Flyway form (e.g. {@code classpath:db/qavo/security})
 * @param owner    the contributing module's identifier, for diagnostics
 */
public record MigrationLocation(String location, String owner) {

    public static MigrationLocation of(String location, String owner) {
        return new MigrationLocation(location, owner);
    }
}
