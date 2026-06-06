-- Rolling-window registration cap ledger owned by the registration plugin (architecture §6, §8).
-- One row per successful registration; the count of rows whose registered_at falls inside the
-- current sliding window is the live registration count consulted by the cap service.
-- Platform migration-version band: V0001–V0099. Registration reserves V0010+; V0010 is taken
-- by the verification-tokens table, so this migration uses V0011.

CREATE TABLE qavo_registration_events (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       VARCHAR(255) NOT NULL,
    registered_at TIMESTAMP    NOT NULL DEFAULT now()
);

-- Range scans on registered_at back every cap query (count + oldest-in-window lookup); the
-- index is therefore mandatory, not optional.
CREATE INDEX idx_registration_events_registered_at
    ON qavo_registration_events (registered_at);
