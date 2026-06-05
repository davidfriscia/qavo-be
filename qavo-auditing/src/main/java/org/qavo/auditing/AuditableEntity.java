/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auditing;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base class for JPA entities that should track who created/modified them and when. Extend this
 * instead of duplicating audit columns on every entity; Spring Data Auditing populates the
 * fields automatically once {@code @EnableJpaAuditing} is active (provided by
 * {@code QavoAuditingAutoConfiguration}).
 *
 * <p>Audit columns are deliberately {@code Instant} (UTC) and the principal columns are plain
 * {@code String} (the principal's stable id from {@link org.qavo.core.security.AuthenticatedPrincipal#id()}).
 * Mapping the auditor as a JPA association would couple every auditable table to the user table
 * and prevent recording principals that come from non-local strategies (OIDC subjects, system
 * accounts, etc.).
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "last_modified_at", nullable = false)
    private Instant lastModifiedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 128)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 128)
    private String lastModifiedBy;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }
}
