package com.pfe.adminagent.request.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An administrative request (leave, mission order or expense). The type-specific
 * fields are held in {@link #structuredData} (JSONB), populated by the AI from
 * the employee's natural-language conversation.
 */
@Entity
@Table(name = "administrative_requests")
public class AdministrativeRequest {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private RequestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private RequestStatus status = RequestStatus.DRAFT;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "structured_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> structuredData = new HashMap<>();

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AdministrativeRequest() {
    }

    private AdministrativeRequest(Builder b) {
        this.id = UUID.randomUUID();
        this.reference = b.reference;
        this.type = b.type;
        this.status = b.status;
        this.title = b.title;
        this.requesterId = b.requesterId;
        this.conversationId = b.conversationId;
        this.structuredData = b.structuredData != null ? b.structuredData : new HashMap<>();
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

    public static Builder builder() {
        return new Builder();
    }

    // --- getters ---
    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public RequestType getType() { return type; }
    public RequestStatus getStatus() { return status; }
    public String getTitle() { return title; }
    public UUID getRequesterId() { return requesterId; }
    public UUID getConversationId() { return conversationId; }
    public Map<String, Object> getStructuredData() { return structuredData; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getDecidedAt() { return decidedAt; }
    public UUID getDecidedBy() { return decidedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // --- state transitions & mutators ---
    public void setTitle(String title) { this.title = title; }
    public void setStructuredData(Map<String, Object> structuredData) { this.structuredData = structuredData; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }

    public void markSubmitted() {
        this.status = RequestStatus.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    public void markUnderReview() {
        this.status = RequestStatus.UNDER_REVIEW;
    }

    public void decide(RequestStatus decision, UUID approverId) {
        this.status = decision;
        this.decidedBy = approverId;
        this.decidedAt = Instant.now();
    }

    public static final class Builder {
        private String reference;
        private RequestType type;
        private RequestStatus status = RequestStatus.DRAFT;
        private String title;
        private UUID requesterId;
        private UUID conversationId;
        private Map<String, Object> structuredData;

        public Builder reference(String reference) { this.reference = reference; return this; }
        public Builder type(RequestType type) { this.type = type; return this; }
        public Builder status(RequestStatus status) { this.status = status; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder requesterId(UUID requesterId) { this.requesterId = requesterId; return this; }
        public Builder conversationId(UUID conversationId) { this.conversationId = conversationId; return this; }
        public Builder structuredData(Map<String, Object> structuredData) { this.structuredData = structuredData; return this; }

        public AdministrativeRequest build() {
            return new AdministrativeRequest(this);
        }
    }
}
