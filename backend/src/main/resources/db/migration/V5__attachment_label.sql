-- =============================================================================
-- V5 — Tag each attachment with the required document it satisfies, so the AI
-- and the dashboard know which pieces were provided vs. still missing.
-- =============================================================================

ALTER TABLE request_attachments
    ADD COLUMN document_label VARCHAR(160);
