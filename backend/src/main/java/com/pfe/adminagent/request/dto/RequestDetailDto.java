package com.pfe.adminagent.request.dto;

import com.pfe.adminagent.request.domain.AdministrativeRequest;
import com.pfe.adminagent.request.domain.RequestStatus;
import com.pfe.adminagent.request.domain.RequestType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Full request view for the detail page / approver panel: the request itself,
 * its structured data, the AI analysis (with citations), the timeline and attachments.
 */
public record RequestDetailDto(
        UUID id,
        String reference,
        RequestType type,
        RequestStatus status,
        String title,
        UUID requesterId,
        String requesterName,
        String requesterDepartment,
        UUID conversationId,
        Map<String, Object> structuredData,
        Instant createdAt,
        Instant submittedAt,
        Instant decidedAt,
        UUID decidedBy,
        String decidedByName,
        AiAnalysisDto analysis,
        List<RequestEventDto> timeline,
        List<AttachmentDto> attachments
) {
    public static RequestDetailDto of(AdministrativeRequest r,
                                      String requesterName, String requesterDepartment,
                                      String decidedByName, AiAnalysisDto analysis,
                                      List<RequestEventDto> timeline, List<AttachmentDto> attachments) {
        return new RequestDetailDto(
                r.getId(), r.getReference(), r.getType(), r.getStatus(), r.getTitle(),
                r.getRequesterId(), requesterName, requesterDepartment, r.getConversationId(),
                r.getStructuredData(), r.getCreatedAt(), r.getSubmittedAt(), r.getDecidedAt(),
                r.getDecidedBy(), decidedByName, analysis, timeline, attachments);
    }
}
