package com.pfe.adminagent.rag;

import com.pfe.adminagent.config.AiProperties;
import com.pfe.adminagent.rag.dto.RagMatch;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Semantic search over the regulation corpus. Embeds the query with bge-m3 and
 * returns the most relevant chunks, each carrying citation metadata. This is the
 * retrieval building block the AI orchestrator uses to ground its answers.
 */
@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final AiProperties.Rag ragProps;

    public RetrievalService(EmbeddingModel embeddingModel,
                            EmbeddingStore<TextSegment> embeddingStore,
                            AiProperties aiProperties) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.ragProps = aiProperties.getRag();
    }

    public List<RagMatch> search(String query, Integer topK) {
        int k = (topK != null && topK > 0) ? topK : ragProps.getTopK();
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(k)
                .minScore(ragProps.getMinScore())
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        List<RagMatch> matches = result.matches().stream().map(this::toMatch).toList();
        log.debug("RAG search '{}' -> {} matches (k={}, minScore={})",
                query, matches.size(), k, ragProps.getMinScore());
        return matches;
    }

    private RagMatch toMatch(EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();
        var md = segment.metadata();
        return new RagMatch(
                round(match.score()),
                segment.text(),
                md.getString(RagMetadataKeys.DOC_REF),
                md.getString(RagMetadataKeys.TITLE),
                md.getString(RagMetadataKeys.CATEGORY),
                md.getString(RagMetadataKeys.SOURCE),
                parseIndex(md.getString(RagMetadataKeys.INDEX)));
    }

    private static Integer parseIndex(String raw) {
        if (raw == null) return null;
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
