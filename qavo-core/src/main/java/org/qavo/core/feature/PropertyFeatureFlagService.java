/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.feature;

import java.util.Map;

import org.qavo.core.config.QavoProperties;

/**
 * Default {@link FeatureFlagService} backed by the {@code qavo.features.*} configuration map.
 * Evaluated against a snapshot of the bound properties; a flag not present in the map is
 * treated as disabled.
 */
public class PropertyFeatureFlagService implements FeatureFlagService {

    private final QavoProperties properties;

    public PropertyFeatureFlagService(QavoProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isEnabled(String feature) {
        Map<String, Boolean> flags = properties.getFeatures();
        return Boolean.TRUE.equals(flags.get(feature));
    }
}
