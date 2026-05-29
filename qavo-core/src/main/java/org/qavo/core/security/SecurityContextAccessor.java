/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.security;

import java.util.Optional;

/**
 * The single, uniform access point to the current security context (see architecture &sect;5.5).
 *
 * <p>Applications inject this rather than reaching into Spring Security's
 * {@code SecurityContextHolder}, so that the authentication strategy stays an implementation
 * detail. The platform provides a default no-op implementation in the core (anonymous) and a
 * Spring Security-backed implementation in {@code qavo-security}.
 */
public interface SecurityContextAccessor {

    /** The authenticated principal, or empty when the request is anonymous. */
    Optional<AuthenticatedPrincipal> currentPrincipal();

    /** Convenience: whether a non-anonymous principal is present. */
    default boolean isAuthenticated() {
        return currentPrincipal().isPresent();
    }

    /** Convenience: whether the current principal holds the given role. */
    default boolean hasRole(String role) {
        return currentPrincipal().map(p -> p.roles().contains(role)).orElse(false);
    }

    /** Convenience: whether the current principal holds the given permission. */
    default boolean hasPermission(String permission) {
        return currentPrincipal().map(p -> p.permissions().contains(permission)).orElse(false);
    }
}
