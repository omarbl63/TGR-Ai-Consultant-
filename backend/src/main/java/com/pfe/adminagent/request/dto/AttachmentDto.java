package com.pfe.adminagent.request.dto;

import com.pfe.adminagent.request.domain.RequestAttachment;

import java.time.Instant;
import java.util.UUID;

public record AttachmentDto(
        UUID id,
        String filename,
        String contentType,
        long sizeBytes,
        String documentLabel,
        Instant createdAt
) {
    public static AttachmentDto from(RequestAttachment a) {
        return new AttachmentDto(a.getId(), a.getFilename(), a.getContentType(),
                a.getSizeBytes(), a.getDocumentLabel(), a.getCreatedAt());
    }
}
