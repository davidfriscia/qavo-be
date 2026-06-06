/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.infrastructure;

import java.time.Instant;
import java.util.UUID;

import org.qavo.auth.registration.domain.QavoEmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for {@link QavoEmailVerificationToken}. Lookups are always by the SHA-256
 * digest of the raw token; the {@code countRecentForUser} query backs the resend rate limit
 * (token rows are kept long enough after consumption to act as the rate-limit ledger, which is
 * acceptable because the table is small and grows only on opt-in registration).
 */
public interface QavoEmailVerificationTokenRepository
        extends JpaRepository<QavoEmailVerificationToken, String> {

    /** Number of tokens issued for the given user at or after the given instant. */
    long countByUserIdAndCreatedAtGreaterThanEqual(UUID userId, Instant since);
}
