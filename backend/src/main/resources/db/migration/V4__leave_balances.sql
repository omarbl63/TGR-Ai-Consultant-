-- =============================================================================
-- V4 — Leave balances (solde de congés) per employee per year.
-- Lets the AI answer "combien de jours de congé reste-t-il ?" from the database.
-- =============================================================================

CREATE TABLE leave_balances (
    id            UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID             NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    year          INTEGER          NOT NULL,
    entitled_days DOUBLE PRECISION NOT NULL DEFAULT 22,
    used_days     DOUBLE PRECISION NOT NULL DEFAULT 0,
    updated_at    TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT uq_leave_balance UNIQUE (user_id, year),
    CONSTRAINT chk_leave_days CHECK (used_days >= 0 AND entitled_days >= 0)
);

CREATE INDEX idx_leave_balances_user ON leave_balances (user_id);
