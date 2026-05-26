-- Qavo local authentication baseline schema (architecture §5.5, §8).
--
-- Migration-version banding convention: platform modules reserve versions V0001–V0099.
-- The security baseline owns V0001. Applications start their own migrations at V0100 so they
-- never collide with platform versions in the shared Flyway history (see docs/best-practices.md).

CREATE TABLE qavo_roles (
    name VARCHAR(64) PRIMARY KEY
);

CREATE TABLE qavo_role_permissions (
    role_name  VARCHAR(64)  NOT NULL,
    permission VARCHAR(128) NOT NULL,
    CONSTRAINT pk_qavo_role_permissions PRIMARY KEY (role_name, permission),
    CONSTRAINT fk_qavo_role_permissions_role FOREIGN KEY (role_name)
        REFERENCES qavo_roles (name) ON DELETE CASCADE
);

CREATE TABLE qavo_users (
    id                 UUID         PRIMARY KEY,
    username           VARCHAR(128) NOT NULL UNIQUE,
    email              VARCHAR(320) NOT NULL UNIQUE,
    password_hash      VARCHAR(255) NOT NULL,
    enabled            BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN      NOT NULL DEFAULT TRUE,
    email_verified     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP    NOT NULL
);

CREATE TABLE qavo_user_roles (
    user_id   UUID        NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    CONSTRAINT pk_qavo_user_roles PRIMARY KEY (user_id, role_name),
    CONSTRAINT fk_qavo_user_roles_user FOREIGN KEY (user_id)
        REFERENCES qavo_users (id) ON DELETE CASCADE,
    CONSTRAINT fk_qavo_user_roles_role FOREIGN KEY (role_name)
        REFERENCES qavo_roles (name) ON DELETE CASCADE
);

-- Baseline roles. Applications add their own roles/permissions via later migrations.
INSERT INTO qavo_roles (name) VALUES ('ADMIN');
INSERT INTO qavo_roles (name) VALUES ('USER');
INSERT INTO qavo_role_permissions (role_name, permission) VALUES ('ADMIN', 'user:read');
INSERT INTO qavo_role_permissions (role_name, permission) VALUES ('ADMIN', 'user:write');
INSERT INTO qavo_role_permissions (role_name, permission) VALUES ('USER', 'user:read');
