/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.api;

/**
 * Wire response returned by the login and refresh endpoints. Carries the bearer access token,
 * its lifetime in seconds, the rotating refresh token, and the resolved principal so the client
 * has everything it needs in a single round-trip.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String refreshToken,
        AuthenticatedUserView user) {

    /** Convenience factory; standardizes the {@code Bearer} token type the platform issues. */
    public static LoginResponse of(String accessToken, long expiresInSeconds,
                                   String refreshToken, AuthenticatedUserView user) {
        return new LoginResponse(accessToken, "Bearer", expiresInSeconds, refreshToken, user);
    }
}
