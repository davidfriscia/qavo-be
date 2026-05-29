/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.api;

import java.util.Set;

import org.qavo.core.security.AuthenticatedPrincipal;

/**
 * Read-only projection of the authenticated user returned by the login and {@code /me} endpoints.
 */
public record AuthenticatedUserView(
        String id,
        String username,
        Set<String> roles,
        Set<String> permissions) {

    public static AuthenticatedUserView from(AuthenticatedPrincipal principal) {
        return new AuthenticatedUserView(
                principal.id(), principal.username(), principal.roles(), principal.permissions());
    }
}
