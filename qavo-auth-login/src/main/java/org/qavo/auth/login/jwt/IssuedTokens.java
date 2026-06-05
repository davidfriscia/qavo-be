/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.jwt;

import java.time.Instant;

/**
 * The pair of credentials returned to a successful login or refresh: a short-lived signed access
 * token and a longer-lived opaque refresh token. Immutable carrier; the controller maps it to
 * the wire DTO without leaking the timing details unless explicitly requested.
 */
public record IssuedTokens(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt) {
}
