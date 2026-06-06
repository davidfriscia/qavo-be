/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.qavo.auth.registration.domain.RegistrationEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for {@link RegistrationEvent}. The two read paths the cap service walks
 * — count rows within the current window and find the oldest row in the window — are exposed
 * as derived queries. The "verified-only" cap mode is implemented as two steps in the service
 * (fetch user ids in window, then count verified users via the platform user repository) so
 * the entity stays decoupled from {@code QavoUser} and the SQL stays dialect-portable.
 *
 * <p>qavo-design: the verified-only count intentionally avoids a cross-entity JPQL join.
 * Such a join would force a UUID/string cast (the event table stores the user id as VARCHAR
 * to keep the SPI provider-agnostic) and that cast is not portable across H2 (used by the
 * reference-app) and PostgreSQL (used by integration tests).
 */
public interface RegistrationEventRepository extends JpaRepository<RegistrationEvent, Long> {

    /** Number of registrations recorded strictly after {@code windowStart}. */
    long countByRegisteredAtAfter(Instant windowStart);

    /** Oldest registration after {@code windowStart}, used to compute the cap-reopen instant. */
    Optional<RegistrationEvent> findFirstByRegisteredAtAfterOrderByRegisteredAtAsc(Instant windowStart);

    /** User ids of every registration recorded strictly after {@code windowStart}. */
    @Query("select e.userId from RegistrationEvent e where e.registeredAt > :windowStart")
    List<String> findUserIdsInWindow(@Param("windowStart") Instant windowStart);

    /** Events recorded strictly after {@code windowStart} ordered oldest first; paginate to limit cost. */
    @Query("select e from RegistrationEvent e where e.registeredAt > :windowStart order by e.registeredAt asc")
    List<RegistrationEvent> findEventsInWindow(@Param("windowStart") Instant windowStart, Pageable page);
}
