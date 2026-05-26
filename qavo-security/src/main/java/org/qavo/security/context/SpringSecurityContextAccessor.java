/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.context;

import java.util.Optional;

import org.qavo.core.security.AuthenticatedPrincipal;
import org.qavo.core.security.SecurityContextAccessor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Security-backed {@link SecurityContextAccessor}. Reads the current
 * {@link org.springframework.security.core.context.SecurityContext} and adapts it to the uniform
 * {@link AuthenticatedPrincipal}, so application code never touches {@code SecurityContextHolder}
 * directly (see architecture &sect;5.5). Replaces the core's anonymous fallback when
 * {@code qavo-security} is present.
 */
public class SpringSecurityContextAccessor implements SecurityContextAccessor {

    @Override
    public Optional<AuthenticatedPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.of(AuthenticatedPrincipalAdapter.from(authentication));
    }
}
