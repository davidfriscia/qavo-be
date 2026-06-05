/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.jwt;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Server-side record of an issued refresh token. The token's plaintext value is never persisted;
 * only its SHA-256 hash is stored, so a database leak cannot be used to mint sessions. The token
 * is single-use: every successful refresh rotates the hash and stamps {@link #revokedAt} on the
 * old row.
 */
@Entity
@Table(name = "qavo_refresh_tokens")
public class RefreshToken {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** SHA-256 hex digest of the refresh-token plaintext value. Unique server-side identifier. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /** Identifier of the principal the token belongs to (as a string for strategy neutrality). */
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Set when the token has been rotated or explicitly revoked. Never deleted, for auditability. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected RefreshToken() {
        // Required by JPA.
    }

    public RefreshToken(UUID id, String tokenHash, String userId, Instant issuedAt, Instant expiresAt) {
        this.id = id;
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void revoke(Instant when) {
        if (this.revokedAt == null) {
            this.revokedAt = when;
        }
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }
}
