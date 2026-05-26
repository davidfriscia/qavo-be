-- Email verification tokens owned by the registration plugin (architecture §6, §8).
-- The plugin owns this table; removing the plugin removes the table's ownership.
-- Platform migration-version band: V0001–V0099. Registration reserves V0010+.

CREATE TABLE qavo_email_verification_tokens (
    token      VARCHAR(128) PRIMARY KEY,
    user_id    UUID         NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    consumed   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL,
    CONSTRAINT fk_qavo_email_verification_user FOREIGN KEY (user_id)
        REFERENCES qavo_users (id) ON DELETE CASCADE
);

CREATE INDEX idx_qavo_email_verification_user ON qavo_email_verification_tokens (user_id);
