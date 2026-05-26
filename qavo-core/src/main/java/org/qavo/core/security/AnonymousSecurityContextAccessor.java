/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.security;

import java.util.Optional;

/**
 * Fallback {@link SecurityContextAccessor} used when no security module is on the classpath.
 * Always reports an anonymous context, allowing the core to be exercised in isolation (for
 * example in lightweight tests) without pulling in Spring Security.
 *
 * <p>When {@code qavo-security} is present it supplies the real, Spring Security-backed
 * implementation, which takes precedence over this one.
 */
public class AnonymousSecurityContextAccessor implements SecurityContextAccessor {

    @Override
    public Optional<AuthenticatedPrincipal> currentPrincipal() {
        return Optional.empty();
    }
}
