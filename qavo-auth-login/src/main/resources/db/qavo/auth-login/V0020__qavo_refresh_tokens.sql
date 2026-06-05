-- Qavo local login plugin — refresh-token storage (architecture §5.5, §8).
--
-- Migration-version banding convention: platform modules reserve V0001–V0099.
-- The login plugin owns V0020–V0029 (security: V0001+, registration: V0010+).
-- Refresh tokens are stored only as SHA-256 hex digests so a database leak cannot be used to
-- mint sessions; the plaintext returned to the client is never persisted.

CREATE TABLE qavo_refresh_tokens (
    id          UUID         PRIMARY KEY,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    user_id     VARCHAR(64)  NOT NULL,
    issued_at   TIMESTAMP    NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    revoked_at  TIMESTAMP
);

CREATE INDEX idx_qavo_refresh_tokens_user_id ON qavo_refresh_tokens (user_id);
CREATE INDEX idx_qavo_refresh_tokens_expires_at ON qavo_refresh_tokens (expires_at);
