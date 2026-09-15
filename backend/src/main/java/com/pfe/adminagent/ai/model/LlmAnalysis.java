package com.pfe.adminagent.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Raw analysis as returned by the LLM (strings/lists), later mapped to the
 * {@code AiAnalysis} entity with defensive enum parsing.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmAnalysis(
        String summary,
        String complianceStatus,
        Double confidenceScore,
        Double riskScore,
        String riskLevel,
        String recommendation,
        String reasoning,
        List<String> missingInfo,
        List<String> missingDocuments,
        List<String> anomalies
) {
}
