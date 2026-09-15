-- =============================================================================
-- V2 — RAG document registry.
-- Tracks each ingested source document for traceability and idempotency.
-- The vector chunks themselves live in the `regulation_embeddings` table,
-- created and managed at runtime by the LangChain4j PgVector embedding store.
-- =============================================================================

CREATE TABLE rag_documents (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    filename        VARCHAR(255)    NOT NULL,
    title           VARCHAR(255),
    doc_ref         VARCHAR(64),
    category        VARCHAR(120),
    content_type    VARCHAR(120),
    size_bytes      BIGINT          NOT NULL DEFAULT 0,
    chunk_count     INTEGER         NOT NULL DEFAULT 0,
    status          VARCHAR(24)     NOT NULL DEFAULT 'PENDING',
    checksum        VARCHAR(64)     NOT NULL,
    error_message   TEXT,
    uploaded_by     UUID,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_rag_documents_checksum UNIQUE (checksum),
    CONSTRAINT chk_rag_documents_status CHECK (status IN ('PENDING', 'INDEXED', 'FAILED')),
    CONSTRAINT fk_rag_documents_uploader FOREIGN KEY (uploaded_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_rag_documents_category ON rag_documents (category);
CREATE INDEX idx_rag_documents_status ON rag_documents (status);
