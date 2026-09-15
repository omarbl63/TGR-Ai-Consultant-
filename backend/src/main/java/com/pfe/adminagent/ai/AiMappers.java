package com.pfe.adminagent.ai;

import com.pfe.adminagent.rag.dto.RagMatch;
import com.pfe.adminagent.request.dto.CitationDto;

final class AiMappers {

    private AiMappers() {
    }

    static CitationDto toCitation(RagMatch m) {
        String excerpt = m.text() != null && m.text().length() > 500
                ? m.text().substring(0, 500) + "…" : m.text();
        return new CitationDto(m.docRef(), m.title(), m.source(), m.chunkIndex(), m.score(), excerpt);
    }
}
