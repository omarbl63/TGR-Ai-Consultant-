package com.pfe.adminagent.conversation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A single message in a {@link Conversation}. {@code metadata} may carry
 * AI artefacts (detected intent, extracted entities, created request id…).
 */
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Message() {
    }

    public Message(UUID conversationId, MessageRole role, String content, Map<String, Object> metadata) {
        this.id = UUID.randomUUID();
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.metadata = metadata;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getConversationId() { return conversationId; }
    public MessageRole getRole() { return role; }
    public String getContent() { return content; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
}
