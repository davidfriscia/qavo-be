/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auditing;

import java.util.Optional;

import org.qavo.core.security.SecurityContextAccessor;
import org.springframework.data.domain.AuditorAware;

/**
 * Resolves the auditor for Spring Data JPA Auditing from the platform's strategy-independent
 * {@link SecurityContextAccessor}: same hook for local, OIDC, or hybrid auth. Unauthenticated
 * writes (background jobs, migrations applied via the application) are recorded as the
 * configured {@code systemPrincipal} so the audit columns are never null in production.
 */
public class QavoAuditorAware implements AuditorAware<String> {

    private final SecurityContextAccessor securityContextAccessor;
    private final String systemPrincipal;

    public QavoAuditorAware(SecurityContextAccessor securityContextAccessor, String systemPrincipal) {
        this.securityContextAccessor = securityContextAccessor;
        this.systemPrincipal = systemPrincipal;
    }

    @Override
    public Optional<String> getCurrentAuditor() {
        return securityContextAccessor.currentPrincipal()
                .map(principal -> principal.id())
                .or(() -> Optional.ofNullable(systemPrincipal));
    }
}
