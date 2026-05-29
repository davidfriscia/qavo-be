/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security;

/**
 * The authentication strategies the platform supports (see architecture &sect;5.5). The strategy
 * selects the authentication mechanism only; the authorization model and the security-context
 * abstraction are uniform across all of them.
 */
public enum AuthenticationStrategy {

    /** DB-backed local authentication — the out-of-the-box default. */
    LOCAL,

    /** Delegate to an external OIDC/OAuth2 provider (Entra ID, Keycloak, any compliant IdP). */
    OIDC,

    /** Local accounts and one or more external providers active simultaneously. */
    HYBRID;

    public boolean includesLocal() {
        return this == LOCAL || this == HYBRID;
    }

    public boolean includesOidc() {
        return this == OIDC || this == HYBRID;
    }
}
