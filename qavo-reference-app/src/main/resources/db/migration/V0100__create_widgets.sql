-- Application-owned schema for the widget catalog.
-- Applications start their migration versions at V0100 so they never collide with the platform's
-- reserved V0001–V0099 band in the shared Flyway history (see docs/best-practices.md).

CREATE TABLE widgets (
    id          UUID         PRIMARY KEY,
    code        VARCHAR(128) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(2000)
);
