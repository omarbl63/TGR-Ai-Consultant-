package com.pfe.adminagent.rag.dto;

import java.util.List;

/**
 * Summary of a knowledge-base re-index run.
 */
public record ReindexResult(
        int filesFound,
        int ingested,
        int skipped,
        int failed,
        List<RagDocumentDto> documents
) {
}
