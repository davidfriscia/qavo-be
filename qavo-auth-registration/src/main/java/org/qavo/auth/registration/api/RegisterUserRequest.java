/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.qavo.validation.constraints.StrongPassword;

/**
 * Self-service registration payload. Validated at the boundary using both standard Jakarta
 * constraints and the platform's reusable {@link StrongPassword} constraint (architecture &sect;5.4).
 */
public record RegisterUserRequest(
        @NotBlank @Size(min = 3, max = 128) String username,
        @NotBlank @Email String email,
        @NotBlank @StrongPassword String password) {
}
