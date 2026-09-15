package com.pfe.adminagent.request.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadata for a supporting document attached to a request.
 * The binary is stored on disk at {@link #storagePath}.
 */
@Entity
@Table(name = "request_attachments")
public class RequestAttachment {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    /** Which required document this attachment satisfies (may be null for extra pieces). */
    @Column(name = "document_label", length = 160)
    private String documentLabel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RequestAttachment() {
    }

    public RequestAttachment(UUID requestId, String filename, String contentType, long sizeBytes,
                             String storagePath, String documentLabel) {
        this.id = UUID.randomUUID();
        this.requestId = requestId;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storagePath = storagePath;
        this.documentLabel = documentLabel;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRequestId() { return requestId; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getStoragePath() { return storagePath; }
    public String getDocumentLabel() { return documentLabel; }
    public Instant getCreatedAt() { return createdAt; }
}
