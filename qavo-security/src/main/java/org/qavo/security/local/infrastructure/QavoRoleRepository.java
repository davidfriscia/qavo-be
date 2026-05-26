/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.local.infrastructure;

import java.util.Optional;

import org.qavo.security.local.domain.QavoRole;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for {@link QavoRole}. Part of the local-auth baseline.
 */
public interface QavoRoleRepository extends JpaRepository<QavoRole, String> {

    Optional<QavoRole> findByName(String name);
}
