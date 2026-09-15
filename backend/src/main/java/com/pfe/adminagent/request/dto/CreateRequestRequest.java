package com.pfe.adminagent.request.dto;

import com.pfe.adminagent.request.domain.RequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

/**
 * Create a request. In production the AI orchestrator fills {@code structuredData}
 * from the conversation; this endpoint also allows direct/manual creation.
 */
public record CreateRequestRequest(
        @NotNull RequestType type,
        @NotBlank @Size(max = 200) String title,
        Map<String, Object> structuredData,
        UUID conversationId
) {
}
