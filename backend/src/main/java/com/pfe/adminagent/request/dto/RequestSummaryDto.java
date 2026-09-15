package com.pfe.adminagent.request.dto;

import com.pfe.adminagent.request.domain.AdministrativeRequest;
import com.pfe.adminagent.request.domain.Recommendation;
import com.pfe.adminagent.request.domain.RequestStatus;
import com.pfe.adminagent.request.domain.RequestType;
import com.pfe.adminagent.request.domain.RiskLevel;

import java.time.Instant;
import java.util.UUID;

/**
 * Compact request view for lists and the approver queue.
 */
public record RequestSummaryDto(
        UUID id,
        String reference,
        RequestType type,
        RequestStatus status,
        String title,
        UUID requesterId,
        String requesterName,
        boolean hasAnalysis,
        Recommendation recommendation,
        RiskLevel riskLevel,
        Double confidenceScore,
        Instant createdAt,
        Instant submittedAt
) {
    public static RequestSummaryDto of(AdministrativeRequest r, String requesterName,
                                       Recommendation recommendation, RiskLevel riskLevel,
                                       Double confidenceScore, boolean hasAnalysis) {
        return new RequestSummaryDto(r.getId(), r.getReference(), r.getType(), r.getStatus(),
                r.getTitle(), r.getRequesterId(), requesterName, hasAnalysis,
                recommendation, riskLevel, confidenceScore, r.getCreatedAt(), r.getSubmittedAt());
    }
}
