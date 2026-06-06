/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One single-use email verification token persisted in {@code qavo_email_verification_tokens}.
 * Only a SHA-256 hex digest of the raw token reaches the database (mirroring the refresh-token
 * convention used by qavo-auth-login); the raw value is sent once in the verification email and
 * never recoverable from the row.
 *
 * <p>State transitions are intentionally explicit:
 * <ul>
 *   <li>created → expires_at &gt; now() &amp;&amp; !consumed</li>
 *   <li>consumed → consumed=true (single-use; resending issues a fresh row)</li>
 *   <li>expired → expires_at &lt;= now() (purgeable by housekeeping outside the platform)</li>
 * </ul>
 */
@Entity
@Table(name = "qavo_email_verification_tokens")
public class QavoEmailVerificationToken {

    @Id
    @Column(name = "token", nullable = false, length = 128)
    private String tokenHash;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed", nullable = false)
    private boolean consumed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected QavoEmailVerificationToken() {
        // Required by JPA.
    }

    public QavoEmailVerificationToken(String tokenHash, UUID userId, Instant expiresAt, Instant createdAt) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.consumed = false;
        this.createdAt = createdAt;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Marks the token as used. Single-use: callers must not unmark it. */
    public void consume() {
        this.consumed = true;
    }
}
