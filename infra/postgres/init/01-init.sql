-- =============================================================================
-- PostgreSQL initialization for AdminAI
-- Runs ONCE, automatically, the first time the data volume is created.
-- Flyway (in the backend) owns the application schema; this file only enables
-- the extensions Flyway and the RAG pipeline rely on.
-- =============================================================================

-- Vector similarity search (embeddings storage & ANN search).
CREATE EXTENSION IF NOT EXISTS vector;

-- Trigram + fuzzy text search helpers (used for hybrid/keyword search later).
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- UUID generation (gen_random_uuid()) — available via pgcrypto.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Sanity log so the extension state is visible in `docker compose logs postgres`.
DO $$
BEGIN
    RAISE NOTICE 'AdminAI init: extensions vector, pg_trgm, pgcrypto are ready.';
END $$;
