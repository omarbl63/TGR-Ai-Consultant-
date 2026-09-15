package com.pfe.adminagent.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.adminagent.ai.dto.AskResponse;
import com.pfe.adminagent.ai.model.LlmAnalysis;
import com.pfe.adminagent.ai.prompt.Prompts;
import com.pfe.adminagent.common.exception.ResourceNotFoundException;
import com.pfe.adminagent.config.AiProperties;
import com.pfe.adminagent.rag.RetrievalService;
import com.pfe.adminagent.rag.dto.RagMatch;
import com.pfe.adminagent.request.RequestService;
import com.pfe.adminagent.request.domain.AdministrativeRequest;
import com.pfe.adminagent.request.domain.AiAnalysis;
import com.pfe.adminagent.request.domain.AiCitation;
import com.pfe.adminagent.request.domain.ComplianceStatus;
import com.pfe.adminagent.request.domain.Recommendation;
import com.pfe.adminagent.request.domain.RequestType;
import com.pfe.adminagent.request.domain.RiskLevel;
import com.pfe.adminagent.request.dto.AiAnalysisDto;
import com.pfe.adminagent.request.dto.CitationDto;
import com.pfe.adminagent.request.repository.AdministrativeRequestRepository;
import com.pfe.adminagent.request.repository.AiAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Produces the explainable AI analysis of a request: retrieves the relevant
 * regulations (RAG), asks the LLM to validate compliance and score risk, then
 * persists the analysis with its citations. The recommendation is advisory only.
 */
@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);
    private static final int RETRIEVAL_K = 6;

    private final AdministrativeRequestRepository requests;
    private final AiAnalysisRepository analyses;
    private final RetrievalService retrievalService;
    private final LlmService llmService;
    private final RequestService requestService;
    private final ObjectMapper objectMapper;
    private final String modelName;

    public AnalysisService(AdministrativeRequestRepository requests,
                           AiAnalysisRepository analyses,
                           RetrievalService retrievalService,
                           LlmService llmService,
                           RequestService requestService,
                           ObjectMapper objectMapper,
                           AiProperties aiProperties) {
        this.requests = requests;
        this.analyses = analyses;
        this.retrievalService = retrievalService;
        this.llmService = llmService;
        this.requestService = requestService;
        this.objectMapper = objectMapper;
        this.modelName = aiProperties.getLlm().getModel();
    }

    /**
     * Answers an approver's question about a specific request, grounded in the
     * request data, its existing AI analysis, and freshly retrieved regulations.
     */
    public AskResponse askAboutRequest(UUID requestId, String question) {
        AdministrativeRequest request = requests.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable : " + requestId));
        List<RagMatch> regulations = retrievalService.search(question + " " + buildQuery(request), RETRIEVAL_K);
        String reasoning = analyses.findByRequestId(requestId)
                .map(a -> a.getReasoning() == null ? "" : a.getReasoning()).orElse("");

        String userPrompt = "Contexte de la demande (JSON):\n" + toJson(request.getStructuredData())
                + "\n\nAnalyse IA existante:\n" + (reasoning.isBlank() ? "(aucune)" : reasoning)
                + "\n\nExtraits de règlement:\n" + Prompts.formatRegulations(regulations)
                + "\n\nQuestion du responsable:\n" + question + "\n\nRéponse (concise):";
        String answer = llmService.complete(Prompts.REQUEST_QA_SYSTEM, userPrompt);
        return new AskResponse(answer, regulations.stream().map(AiMappers::toCitation).toList());
    }

    public AiAnalysisDto analyze(UUID requestId) {
        AdministrativeRequest request = requests.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable : " + requestId));

        List<RagMatch> regulations = retrievalService.search(buildQuery(request), RETRIEVAL_K);
        String structuredJson = toJson(request.getStructuredData());

        LlmAnalysis llm = llmService.completeJson(
                Prompts.ANALYSIS_SYSTEM,
                Prompts.analysisUser(request.getType(), structuredJson, regulations),
                LlmAnalysis.class);

        AiAnalysis analysis = new AiAnalysis(requestId);
        analysis.setSummary(llm.summary());
        analysis.setConfidenceScore(clamp(llm.confidenceScore()));
        analysis.setRiskScore(clamp(llm.riskScore()));
        analysis.setRiskLevel(Enums.parse(RiskLevel.class, llm.riskLevel()));
        analysis.setComplianceStatus(Enums.parse(ComplianceStatus.class, llm.complianceStatus()));
        analysis.setRecommendation(Enums.parse(Recommendation.class, llm.recommendation()));
        analysis.setReasoning(llm.reasoning());
        // Missing info & documents are computed deterministically from what the employee
        // actually provided (so information/pieces already given are NOT shown as missing).
        analysis.setMissingInfo(computeMissingInfo(request));
        analysis.setMissingDocuments(computeMissingDocuments(requestId, request.getType()));
        analysis.setAnomalies(nonNull(llm.anomalies()));
        analysis.setModel(modelName);

        List<AiCitation> citations = regulations.stream()
                .map(m -> new AiCitation(analysis.getId(), m.docRef(), m.title(), m.source(),
                        m.chunkIndex(), m.score(), truncate(m.text())))
                .toList();

        requestService.saveAnalysis(analysis, citations);
        log.info("AI analysis for {} -> recommendation={}, risk={}, compliance={}",
                request.getReference(), analysis.getRecommendation(), analysis.getRiskLevel(),
                analysis.getComplianceStatus());

        List<CitationDto> citationDtos = citations.stream().map(CitationDto::from).toList();
        return AiAnalysisDto.of(analysis, citationDtos);
    }

    /** Required fields whose value is missing/blank in the submitted data. */
    private List<String> computeMissingInfo(AdministrativeRequest request) {
        Map<String, Object> data = request.getStructuredData();
        List<String> missing = new ArrayList<>();
        for (RequestTypeSpec.Field f : RequestTypeSpec.requiredFields(request.getType())) {
            Object v = data == null ? null : data.get(f.key());
            if (v == null || (v instanceof String s && s.isBlank())) {
                missing.add(f.label());
            }
        }
        return missing;
    }

    /** Required documents that the employee did NOT attach. */
    private List<String> computeMissingDocuments(UUID requestId, RequestType type) {
        java.util.Set<String> provided = requestService.providedDocumentLabels(requestId);
        List<String> missing = new ArrayList<>();
        for (String doc : RequestTypeSpec.requiredDocuments(type)) {
            if (!provided.contains(doc)) {
                missing.add(doc);
            }
        }
        return missing;
    }

    private String buildQuery(AdministrativeRequest request) {
        RequestType type = request.getType();
        StringBuilder q = new StringBuilder(RequestTypeSpec.label(type))
                .append(' ').append(RequestTypeSpec.retrievalHint(type));
        Map<String, Object> data = request.getStructuredData();
        if (data != null) {
            for (String key : List.of("destination", "purpose", "leaveType", "category")) {
                Object v = data.get(key);
                if (v != null) {
                    q.append(' ').append(v);
                }
            }
        }
        return q.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static List<String> nonNull(List<String> list) {
        return list != null ? list : new ArrayList<>();
    }

    private static Double clamp(Double v) {
        if (v == null) {
            return null;
        }
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > 1000 ? text.substring(0, 1000) + "…" : text;
    }
}
