/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.local.lockout;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;

/**
 * Translates Spring Security's authentication lifecycle events into {@link LockoutService} calls.
 * The wiring lives outside the service so the service stays unit-testable and so the listener can
 * react uniformly to any provider (DAO, custom), not only the default username/password path.
 *
 * <p>Failure events are scoped to {@link AbstractAuthenticationFailureEvent} so we count every
 * kind of failed authentication against the same threshold — a deliberate choice that mirrors
 * standard lockout policies (e.g. NIST SP 800-63B §5.2.2): treating only "bad password" failures
 * as relevant would let attackers probe usernames with malformed requests cheaply.
 */
public class AuthenticationEventListener {

    private final LockoutService lockoutService;

    public AuthenticationEventListener(LockoutService lockoutService) {
        this.lockoutService = lockoutService;
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        Authentication authentication = event.getAuthentication();
        if (authentication == null) {
            return;
        }
        lockoutService.recordFailure(authentication.getName());
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        if (authentication == null) {
            return;
        }
        lockoutService.recordSuccess(authentication.getName());
    }
}
