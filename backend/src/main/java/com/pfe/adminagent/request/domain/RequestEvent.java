package com.pfe.adminagent.request.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * An immutable entry in a request's approval timeline.
 */
@Entity
@Table(name = "request_events")
public class RequestEvent {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RequestEventType type;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RequestEvent() {
    }

    public RequestEvent(UUID requestId, RequestEventType type, UUID actorId, String comment) {
        this.id = UUID.randomUUID();
        this.requestId = requestId;
        this.type = type;
        this.actorId = actorId;
        this.comment = comment;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRequestId() { return requestId; }
    public RequestEventType getType() { return type; }
    public UUID getActorId() { return actorId; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
}
