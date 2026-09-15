package com.pfe.adminagent.audit.dto;

import com.pfe.adminagent.audit.domain.AuditLog;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogDto(
        UUID id,
        UUID actorId,
        String action,
        String entityType,
        String entityId,
        Map<String, Object> details,
        Instant createdAt
) {
    public static AuditLogDto from(AuditLog a) {
        return new AuditLogDto(a.getId(), a.getActorId(), a.getAction(), a.getEntityType(),
                a.getEntityId(), a.getDetails(), a.getCreatedAt());
    }
}
