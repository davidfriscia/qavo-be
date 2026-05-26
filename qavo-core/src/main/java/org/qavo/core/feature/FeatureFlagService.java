/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.feature;

/**
 * Lightweight feature-flag evaluation (see architecture &sect;5.6).
 *
 * <p>This is deliberately minimal — it is not a replacement for a full feature-management
 * platform — but it covers environment-aware rollout without a rebuild. Static flags are read
 * from the {@code qavo.features.*} configuration; the abstraction leaves room for a future
 * dynamic source (a DB table or remote provider) behind the same interface.
 */
public interface FeatureFlagService {

    /** Whether the named feature is currently enabled. Unknown features are disabled. */
    boolean isEnabled(String feature);

    /** Whether the named feature is disabled (convenience inverse of {@link #isEnabled}). */
    default boolean isDisabled(String feature) {
        return !isEnabled(feature);
    }
}
