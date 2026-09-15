package com.pfe.adminagent.conversation.dto;

import com.pfe.adminagent.conversation.domain.Message;
import com.pfe.adminagent.conversation.domain.MessageRole;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MessageDto(
        UUID id,
        MessageRole role,
        String content,
        Map<String, Object> metadata,
        Instant createdAt
) {
    public static MessageDto from(Message m) {
        return new MessageDto(m.getId(), m.getRole(), m.getContent(), m.getMetadata(), m.getCreatedAt());
    }
}
