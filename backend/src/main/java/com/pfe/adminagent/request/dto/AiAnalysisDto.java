package com.pfe.adminagent.request.dto;

import com.pfe.adminagent.request.domain.AiAnalysis;
import com.pfe.adminagent.request.domain.ComplianceStatus;
import com.pfe.adminagent.request.domain.Recommendation;
import com.pfe.adminagent.request.domain.RiskLevel;

import java.time.Instant;
import java.util.List;

/**
 * The AI analysis presented to the approver — the explainability payload.
 */
public record AiAnalysisDto(
        String summary,
        Double confidenceScore,
        Double riskScore,
        RiskLevel riskLevel,
        ComplianceStatus complianceStatus,
        Recommendation recommendation,
        String reasoning,
        List<String> missingInfo,
        List<String> missingDocuments,
        List<String> anomalies,
        String model,
        Instant createdAt,
        List<CitationDto> citations
) {
    public static AiAnalysisDto of(AiAnalysis a, List<CitationDto> citations) {
        return new AiAnalysisDto(
                a.getSummary(), a.getConfidenceScore(), a.getRiskScore(), a.getRiskLevel(),
                a.getComplianceStatus(), a.getRecommendation(), a.getReasoning(),
                a.getMissingInfo(), a.getMissingDocuments(), a.getAnomalies(),
                a.getModel(), a.getCreatedAt(), citations);
    }
}
