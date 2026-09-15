-- =============================================================================
-- V1 — Core identity schema.
-- Extensions (vector, pgcrypto, pg_trgm) are enabled by infra/postgres/init.
-- =============================================================================

CREATE TABLE users (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(160)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    full_name       VARCHAR(160)    NOT NULL,
    role            VARCHAR(32)      NOT NULL,
    department      VARCHAR(120),
    job_title       VARCHAR(120),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_role CHECK (role IN ('EMPLOYEE', 'MANAGER', 'DIRECTOR', 'ADMIN'))
);

CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_department ON users (department);
