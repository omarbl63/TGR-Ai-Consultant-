package com.pfe.adminagent.rag.dto;

import com.pfe.adminagent.rag.domain.DocumentStatus;
import com.pfe.adminagent.rag.domain.RagDocument;

import java.time.Instant;
import java.util.UUID;

public record RagDocumentDto(
        UUID id,
        String filename,
        String title,
        String docRef,
        String category,
        String contentType,
        long sizeBytes,
        int chunkCount,
        DocumentStatus status,
        String errorMessage,
        Instant createdAt
) {
    public static RagDocumentDto from(RagDocument d) {
        return new RagDocumentDto(
                d.getId(), d.getFilename(), d.getTitle(), d.getDocRef(), d.getCategory(),
                d.getContentType(), d.getSizeBytes(), d.getChunkCount(), d.getStatus(),
                d.getErrorMessage(), d.getCreatedAt());
    }
}
