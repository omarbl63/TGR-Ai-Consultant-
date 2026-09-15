package com.pfe.adminagent.ai.web;

import com.pfe.adminagent.ai.AiOrchestrator;
import com.pfe.adminagent.ai.AnalysisService;
import com.pfe.adminagent.ai.QaService;
import com.pfe.adminagent.ai.dto.AskRequest;
import com.pfe.adminagent.ai.dto.AskResponse;
import com.pfe.adminagent.ai.dto.ChatRequest;
import com.pfe.adminagent.ai.dto.ChatResponse;
import com.pfe.adminagent.request.dto.AiAnalysisDto;
import com.pfe.adminagent.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * AI Orchestrator endpoints — the primary interface of the platform.
 */
@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI Agent", description = "Conversational agent, RAG Q&A, and request analysis")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final AiOrchestrator orchestrator;
    private final QaService qaService;
    private final AnalysisService analysisService;

    public AiController(AiOrchestrator orchestrator, QaService qaService, AnalysisService analysisService) {
        this.orchestrator = orchestrator;
        this.qaService = qaService;
        this.analysisService = analysisService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Talk to the AI agent (creates/tracks requests from natural language)")
    public ChatResponse chat(@Valid @RequestBody ChatRequest body,
                             @AuthenticationPrincipal UserPrincipal principal) {
        return orchestrator.chat(principal.getId(), body.conversationId(), body.message());
    }

    @PostMapping("/ask")
    @Operation(summary = "Ask an administrative question (RAG-grounded, with citations)")
    public AskResponse ask(@Valid @RequestBody AskRequest body) {
        return qaService.ask(body.question(), body.topK());
    }

    @PostMapping("/requests/{id}/analyze")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Run (or re-run) the AI compliance analysis for a request")
    public AiAnalysisDto analyze(@PathVariable UUID id) {
        return analysisService.analyze(id);
    }

    @PostMapping("/requests/{id}/ask")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Ask the AI about a specific request (e.g. why it recommends approval)")
    public AskResponse askAboutRequest(@PathVariable UUID id, @Valid @RequestBody AskRequest body) {
        return analysisService.askAboutRequest(id, body.question());
    }
}
