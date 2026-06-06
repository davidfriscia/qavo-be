/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One row per successful self-service registration, persisted in
 * {@code qavo_registration_events}. The table itself is the audit trail consulted by the
 * registration cap service; no audit columns of its own are needed.
 */
@Entity
@Table(name = "qavo_registration_events")
public class RegistrationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    protected RegistrationEvent() {
        // Required by JPA.
    }

    public RegistrationEvent(String userId, Instant registeredAt) {
        this.userId = userId;
        this.registeredAt = registeredAt;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }
}
