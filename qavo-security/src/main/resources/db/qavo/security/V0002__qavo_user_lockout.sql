-- Account-lockout columns for the local user store (architecture §5.5, ADR 0007).
--
-- Reserves platform Flyway band V0002 within qavo-security's V0001–V0099 range. Existing rows
-- get the safe defaults: zero failed attempts and no active lock.

ALTER TABLE qavo_users ADD COLUMN failed_login_attempts INT       NOT NULL DEFAULT 0;
ALTER TABLE qavo_users ADD COLUMN locked_until          TIMESTAMP NULL;

CREATE INDEX idx_qavo_users_locked_until ON qavo_users (locked_until);
