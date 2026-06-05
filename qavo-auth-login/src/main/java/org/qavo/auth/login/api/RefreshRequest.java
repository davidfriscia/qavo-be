/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Body submitted to the refresh endpoint. The refresh token is the opaque, server-issued
 * Base64URL value originally returned by login or by a previous refresh.
 */
public record RefreshRequest(@NotBlank String refreshToken) {
}
