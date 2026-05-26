/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.api;

import java.util.UUID;

import org.qavo.security.local.domain.QavoUser;

/**
 * Response returned after a successful registration. Deliberately excludes any credential material.
 */
public record RegisteredUserView(UUID id, String username, String email, boolean emailVerified) {

    public static RegisteredUserView from(QavoUser user) {
        return new RegisteredUserView(user.getId(), user.getUsername(), user.getEmail(), user.isEmailVerified());
    }
}
