/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.web;

import java.util.List;

/**
 * Lets a plugin declare endpoints that must be reachable without authentication (architecture
 * &sect;6). The base filter chain merges every contributor's paths with the configured
 * {@code qavo.security.public-paths} before applying the {@code authenticated()} catch-all, so a
 * plugin can open exactly its own routes (e.g. the registration endpoint) without redefining the
 * whole authorization policy.
 */
@FunctionalInterface
public interface PublicPathContributor {

    /** Ant-style path patterns to permit without authentication. */
    List<String> publicPaths();
}
