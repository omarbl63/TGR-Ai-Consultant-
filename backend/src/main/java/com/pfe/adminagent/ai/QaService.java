package com.pfe.adminagent.ai;

import com.pfe.adminagent.ai.dto.AskResponse;
import com.pfe.adminagent.ai.prompt.Prompts;
import com.pfe.adminagent.rag.RetrievalService;
import com.pfe.adminagent.rag.dto.RagMatch;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Retrieval-augmented administrative Q&A: retrieve regulations, then answer
 * grounded strictly in them, with citations.
 */
@Service
public class QaService {

    private final RetrievalService retrievalService;
    private final LlmService llmService;

    public QaService(RetrievalService retrievalService, LlmService llmService) {
        this.retrievalService = retrievalService;
        this.llmService = llmService;
    }

    public AskResponse ask(String question, Integer topK) {
        List<RagMatch> matches = retrievalService.search(question, topK);
        String answer = llmService.complete(Prompts.QA_SYSTEM, Prompts.qaUser(question, matches));
        return new AskResponse(answer, matches.stream().map(AiMappers::toCitation).toList());
    }
}
