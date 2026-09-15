-- =============================================================================
-- V3 — Core administrative domain.
-- Conversations & messages (the AI chat), administrative requests, attachments,
-- AI analyses + citations, request timeline events, notifications, audit log.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Conversations & messages
-- ---------------------------------------------------------------------------
CREATE TABLE conversations (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title       VARCHAR(200),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_conversations_user ON conversations (user_id, updated_at DESC);

CREATE TABLE messages (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID        NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    role            VARCHAR(16) NOT NULL,
    content         TEXT        NOT NULL,
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_messages_role CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);
CREATE INDEX idx_messages_conversation ON messages (conversation_id, created_at);

-- ---------------------------------------------------------------------------
-- Administrative requests
-- ---------------------------------------------------------------------------
-- Human-readable reference numbers: REQ-<year>-<zero-padded sequence>.
CREATE SEQUENCE request_reference_seq START 1;

CREATE TABLE administrative_requests (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    reference       VARCHAR(32) NOT NULL UNIQUE,
    type            VARCHAR(24) NOT NULL,
    status          VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    title           VARCHAR(200) NOT NULL,
    requester_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    conversation_id UUID        REFERENCES conversations (id) ON DELETE SET NULL,
    structured_data JSONB       NOT NULL DEFAULT '{}'::jsonb,
    submitted_at    TIMESTAMPTZ,
    decided_at      TIMESTAMPTZ,
    decided_by      UUID        REFERENCES users (id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_req_type CHECK (type IN ('LEAVE', 'MISSION_ORDER', 'EXPENSE')),
    CONSTRAINT chk_req_status CHECK (status IN
        ('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'CHANGES_REQUESTED'))
);
CREATE INDEX idx_requests_status ON administrative_requests (status, created_at DESC);
CREATE INDEX idx_requests_requester ON administrative_requests (requester_id, created_at DESC);
CREATE INDEX idx_requests_type ON administrative_requests (type);

CREATE TABLE request_attachments (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id   UUID        NOT NULL REFERENCES administrative_requests (id) ON DELETE CASCADE,
    filename     VARCHAR(255) NOT NULL,
    content_type VARCHAR(120),
    size_bytes   BIGINT      NOT NULL DEFAULT 0,
    storage_path VARCHAR(512) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_attachments_request ON request_attachments (request_id);

-- ---------------------------------------------------------------------------
-- AI analysis (explainability) + citations
-- ---------------------------------------------------------------------------
CREATE TABLE ai_analyses (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id        UUID        NOT NULL UNIQUE REFERENCES administrative_requests (id) ON DELETE CASCADE,
    summary           TEXT,
    confidence_score  DOUBLE PRECISION,
    risk_score        DOUBLE PRECISION,
    risk_level        VARCHAR(16),
    compliance_status VARCHAR(32),
    recommendation    VARCHAR(24),
    reasoning         TEXT,
    missing_info      JSONB       NOT NULL DEFAULT '[]'::jsonb,
    missing_documents JSONB       NOT NULL DEFAULT '[]'::jsonb,
    anomalies         JSONB       NOT NULL DEFAULT '[]'::jsonb,
    model             VARCHAR(120),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_ai_risk_level CHECK (risk_level IS NULL OR risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_ai_recommendation CHECK (recommendation IS NULL OR recommendation IN
        ('APPROVE', 'REJECT', 'REQUEST_CHANGES')),
    CONSTRAINT chk_ai_compliance CHECK (compliance_status IS NULL OR compliance_status IN
        ('COMPLIANT', 'COMPLIANT_WITH_RESERVATION', 'INCOMPLETE', 'NON_COMPLIANT'))
);

CREATE TABLE ai_citations (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    analysis_id UUID        NOT NULL REFERENCES ai_analyses (id) ON DELETE CASCADE,
    doc_ref     VARCHAR(64),
    title       VARCHAR(255),
    source      VARCHAR(255),
    chunk_index INTEGER,
    score       DOUBLE PRECISION,
    excerpt     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_citations_analysis ON ai_citations (analysis_id);

-- ---------------------------------------------------------------------------
-- Request timeline events
-- ---------------------------------------------------------------------------
CREATE TABLE request_events (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id  UUID        NOT NULL REFERENCES administrative_requests (id) ON DELETE CASCADE,
    type        VARCHAR(32) NOT NULL,
    actor_id    UUID        REFERENCES users (id) ON DELETE SET NULL,
    comment     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_event_type CHECK (type IN
        ('CREATED', 'SUBMITTED', 'AI_ANALYZED', 'APPROVED', 'REJECTED', 'CHANGES_REQUESTED', 'COMMENTED'))
);
CREATE INDEX idx_events_request ON request_events (request_id, created_at);

-- ---------------------------------------------------------------------------
-- Notifications
-- ---------------------------------------------------------------------------
CREATE TABLE notifications (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type        VARCHAR(32) NOT NULL,
    title       VARCHAR(200) NOT NULL,
    message     TEXT,
    request_id  UUID        REFERENCES administrative_requests (id) ON DELETE CASCADE,
    is_read     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user ON notifications (user_id, is_read, created_at DESC);

-- ---------------------------------------------------------------------------
-- Audit log
-- ---------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID        REFERENCES users (id) ON DELETE SET NULL,
    action      VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64),
    entity_id   VARCHAR(64),
    details     JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_created ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_actor ON audit_logs (actor_id, created_at DESC);
