package com.pfe.adminagent.rag.dto;

/**
 * A single retrieved chunk with its similarity score and citation metadata.
 */
public record RagMatch(
        double score,
        String text,
        String docRef,
        String title,
        String category,
        String source,
        Integer chunkIndex
) {
}
