/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auditing;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.qavo.core.security.AuthenticatedPrincipal;
import org.qavo.core.security.SecurityContextAccessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link QavoAuditorAware} returns the principal id when one is authenticated and falls
 * back to the configured system principal otherwise — covering the two branches every audit row
 * exercises in production.
 */
class QavoAuditorAwareTest {

    @Test
    void returnsPrincipalIdWhenAuthenticated() {
        QavoAuditorAware aware = new QavoAuditorAware(() -> Optional.of(new TestPrincipal("user-42")), "system");

        assertThat(aware.getCurrentAuditor()).contains("user-42");
    }

    @Test
    void fallsBackToSystemPrincipalWhenAnonymous() {
        QavoAuditorAware aware = new QavoAuditorAware(Optional::empty, "batch-job");

        assertThat(aware.getCurrentAuditor()).contains("batch-job");
    }

    @Test
    void returnsEmptyWhenAnonymousAndNoSystemPrincipalConfigured() {
        QavoAuditorAware aware = new QavoAuditorAware(Optional::empty, null);

        assertThat(aware.getCurrentAuditor()).isEmpty();
    }

    private record TestPrincipal(String id) implements AuthenticatedPrincipal {
        @Override public String username() { return id; }
        @Override public Set<String> roles() { return Set.of(); }
        @Override public Set<String> permissions() { return Set.of(); }
        @Override public java.util.Map<String, Object> attributes() { return java.util.Map.of(); }
    }

    /** Functional sugar so the tests can pass a lambda where a {@link SecurityContextAccessor} is required. */
    @FunctionalInterface
    private interface StubSecurityContext extends SecurityContextAccessor {}
}
