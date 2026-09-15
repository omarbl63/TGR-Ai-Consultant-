package com.pfe.adminagent.request.dto;

import com.pfe.adminagent.request.domain.AiCitation;

public record CitationDto(
        String docRef,
        String title,
        String source,
        Integer chunkIndex,
        Double score,
        String excerpt
) {
    public static CitationDto from(AiCitation c) {
        return new CitationDto(c.getDocRef(), c.getTitle(), c.getSource(),
                c.getChunkIndex(), c.getScore(), c.getExcerpt());
    }
}
