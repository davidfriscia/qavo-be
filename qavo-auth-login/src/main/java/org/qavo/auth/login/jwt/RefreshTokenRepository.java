/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.jwt;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Persistence contract for {@link RefreshToken}. Two access paths matter: looking up by hash to
 * validate a presented refresh token, and revoking every active token of a principal on logout.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken rt set rt.revokedAt = :when "
            + "where rt.userId = :userId and rt.revokedAt is null")
    int revokeAllActiveForUser(@Param("userId") String userId, @Param("when") Instant when);
}
