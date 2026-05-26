/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.local.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.qavo.security.local.domain.QavoUser;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for {@link QavoUser}. Part of the local-auth baseline; plugins such as
 * registration create users through the higher-level service, not this repository directly.
 */
public interface QavoUserRepository extends JpaRepository<QavoUser, UUID> {

    Optional<QavoUser> findByUsername(String username);

    Optional<QavoUser> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
