package com.pfe.adminagent.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Append-only record of a significant action, for traceability and compliance.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "entity_type", length = 64)
    private String entityType;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(UUID actorId, String action, String entityType, String entityId, Map<String, Object> details) {
        this.id = UUID.randomUUID();
        this.actorId = actorId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getActorId() { return actorId; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public Map<String, Object> getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
