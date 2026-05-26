/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.autoconfigure;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.qavo.core.migration.MigrationLocation;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;

/**
 * Aggregates the Flyway migration locations contributed by the platform modules
 * (see architecture &sect;8). Each plugin owns its schema by shipping migrations under its own
 * reserved location and publishing a {@link MigrationLocation} bean; this customizer merges them
 * with whatever the application already configured, so plugin migrations are discovered and run
 * at startup without any per-application wiring.
 *
 * <p>Active only when Flyway is on the classpath, keeping non-persistent applications unaffected.
 */
@AutoConfiguration
@ConditionalOnClass({Flyway.class, FlywayConfigurationCustomizer.class})
public class QavoFlywayAutoConfiguration {

    @Bean
    public FlywayConfigurationCustomizer qavoMigrationLocationCustomizer(
            @Nullable List<MigrationLocation> moduleLocations) {
        List<MigrationLocation> locations = moduleLocations != null ? moduleLocations : List.of();
        return configuration -> {
            Set<String> merged = new LinkedHashSet<>();
            for (Location existing : configuration.getLocations()) {
                merged.add(existing.getDescriptor());
            }
            for (MigrationLocation moduleLocation : locations) {
                merged.add(moduleLocation.location());
            }
            configuration.locations(merged.toArray(new String[0]));
        };
    }
}
