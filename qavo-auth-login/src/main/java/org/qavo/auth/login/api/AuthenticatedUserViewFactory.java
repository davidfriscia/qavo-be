/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.api;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Builds an {@link AuthenticatedUserView} from a freshly authenticated {@link Authentication},
 * applying the same {@code ROLE_}-prefix convention the platform uses to split roles from
 * permissions (architecture &sect;5.5).
 */
final class AuthenticatedUserViewFactory {

    private static final String ROLE_PREFIX = "ROLE_";

    private AuthenticatedUserViewFactory() {
    }

    static AuthenticatedUserView from(Authentication authentication) {
        Set<String> roles = new LinkedHashSet<>();
        Set<String> permissions = new LinkedHashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value.startsWith(ROLE_PREFIX)) {
                roles.add(value.substring(ROLE_PREFIX.length()));
            } else {
                permissions.add(value);
            }
        }
        return new AuthenticatedUserView(authentication.getName(), authentication.getName(), roles, permissions);
    }
}
