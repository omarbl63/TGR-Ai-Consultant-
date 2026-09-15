package com.pfe.adminagent.request.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The AI's explainable analysis of a request: summary, confidence & risk scores,
 * compliance status, detected gaps/anomalies, reasoning and a recommendation.
 * Produced by the orchestrator; reviewed (never blindly trusted) by a human.
 */
@Entity
@Table(name = "ai_analyses")
public class AiAnalysis {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "request_id", nullable = false, unique = true)
    private UUID requestId;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "risk_score")
    private Double riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 16)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_status", length = 32)
    private ComplianceStatus complianceStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private Recommendation recommendation;

    @Column(columnDefinition = "text")
    private String reasoning;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_info", nullable = false, columnDefinition = "jsonb")
    private List<String> missingInfo = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_documents", nullable = false, columnDefinition = "jsonb")
    private List<String> missingDocuments = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> anomalies = new ArrayList<>();

    @Column(length = 120)
    private String model;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AiAnalysis() {
    }

    public AiAnalysis(UUID requestId) {
        this.id = UUID.randomUUID();
        this.requestId = requestId;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRequestId() { return requestId; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public ComplianceStatus getComplianceStatus() { return complianceStatus; }
    public void setComplianceStatus(ComplianceStatus complianceStatus) { this.complianceStatus = complianceStatus; }
    public Recommendation getRecommendation() { return recommendation; }
    public void setRecommendation(Recommendation recommendation) { this.recommendation = recommendation; }
    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    public List<String> getMissingInfo() { return missingInfo; }
    public void setMissingInfo(List<String> missingInfo) { this.missingInfo = missingInfo; }
    public List<String> getMissingDocuments() { return missingDocuments; }
    public void setMissingDocuments(List<String> missingDocuments) { this.missingDocuments = missingDocuments; }
    public List<String> getAnomalies() { return anomalies; }
    public void setAnomalies(List<String> anomalies) { this.anomalies = anomalies; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Instant getCreatedAt() { return createdAt; }
}
