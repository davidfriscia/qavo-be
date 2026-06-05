/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.jwt;

import org.qavo.core.security.AuthenticatedPrincipal;

/**
 * Issues and rotates platform-signed access and refresh tokens for the local authentication
 * strategy (architecture &sect;5.5). Implementations encapsulate the JWT signing material, the
 * refresh-token storage strategy, and the rotation/revocation rules so the calling code only
 * deals with a {@link AuthenticatedPrincipal} and the resulting {@link IssuedTokens}.
 *
 * <p>The contract is deliberately small: a freshly authenticated principal yields a token pair,
 * a presented refresh token is exchanged for a new pair (and the old refresh token is
 * invalidated atomically), and a principal's active sessions can be revoked wholesale on logout.
 */
public interface TokenService {

    /**
     * Issue a new access/refresh pair for the given freshly authenticated principal. Existing
     * refresh tokens for the principal are not touched, so concurrent sessions (e.g. browser and
     * mobile) coexist.
     */
    IssuedTokens issueFor(AuthenticatedPrincipal principal);

    /**
     * Exchange a valid refresh token for a fresh access/refresh pair, atomically revoking the
     * presented token. Implementations throw {@link RefreshTokenException} on any failure so the
     * caller never has to distinguish missing/expired/revoked at the call site.
     */
    IssuedTokens refresh(String refreshToken);

    /** Revoke every active refresh token of the given principal. Logout endpoint hook. */
    void revokeAllFor(AuthenticatedPrincipal principal);
}
