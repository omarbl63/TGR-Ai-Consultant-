package com.pfe.adminagent.request.dto;

import com.pfe.adminagent.request.domain.RequestEvent;
import com.pfe.adminagent.request.domain.RequestEventType;

import java.time.Instant;
import java.util.UUID;

public record RequestEventDto(
        UUID id,
        RequestEventType type,
        UUID actorId,
        String actorName,
        String comment,
        Instant createdAt
) {
    public static RequestEventDto of(RequestEvent e, String actorName) {
        return new RequestEventDto(e.getId(), e.getType(), e.getActorId(), actorName,
                e.getComment(), e.getCreatedAt());
    }
}
