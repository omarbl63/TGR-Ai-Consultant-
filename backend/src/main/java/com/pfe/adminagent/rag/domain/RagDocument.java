package com.pfe.adminagent.rag.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Registry entry for a source document ingested into the RAG pipeline.
 * Vector chunks are stored separately in the embedding store.
 */
@Entity
@Table(name = "rag_documents")
public class RagDocument {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String filename;

    private String title;

    @Column(name = "doc_ref", length = 64)
    private String docRef;

    @Column(length = 120)
    private String category;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RagDocument() {
        // JPA
    }

    public RagDocument(String filename, String checksum) {
        this.id = UUID.randomUUID();
        this.filename = filename;
        this.checksum = checksum;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void markIndexed(int chunkCount) {
        this.chunkCount = chunkCount;
        this.status = DocumentStatus.INDEXED;
        this.errorMessage = null;
    }

    public void markFailed(String message) {
        this.status = DocumentStatus.FAILED;
        this.errorMessage = message != null && message.length() > 2000 ? message.substring(0, 2000) : message;
    }

    // --- getters / setters ---
    public UUID getId() { return id; }
    public String getFilename() { return filename; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDocRef() { return docRef; }
    public void setDocRef(String docRef) { this.docRef = docRef; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public int getChunkCount() { return chunkCount; }
    public DocumentStatus getStatus() { return status; }
    public String getChecksum() { return checksum; }
    public String getErrorMessage() { return errorMessage; }
    public UUID getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(UUID uploadedBy) { this.uploadedBy = uploadedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
