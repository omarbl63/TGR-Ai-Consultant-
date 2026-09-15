package com.pfe.adminagent.ai.dto;

import com.pfe.adminagent.ai.IntentType;
import com.pfe.adminagent.request.domain.Recommendation;
import com.pfe.adminagent.request.dto.CitationDto;

import java.util.List;
import java.util.UUID;

/**
 * The agent's reply to an employee message, plus any side effects (a created
 * request), the citations used, and optional quick-reply {@code choices} the
 * user can tap (e.g. the menu of request types).
 */
public record ChatResponse(
        UUID conversationId,
        String reply,
        IntentType intent,
        UUID createdRequestId,
        String requestReference,
        Recommendation recommendation,
        List<String> missingInformation,
        List<CitationDto> citations,
        List<String> choices,
        boolean awaitingDocuments,
        List<String> requiredDocuments
) {
}
