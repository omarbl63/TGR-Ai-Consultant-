package com.pfe.adminagent.ai.dto;

import com.pfe.adminagent.request.dto.CitationDto;

import java.util.List;

public record AskResponse(
        String answer,
        List<CitationDto> citations
) {
}
