/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Credentials submitted to the local login endpoint. Immutable DTO validated at the boundary.
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password) {
}
