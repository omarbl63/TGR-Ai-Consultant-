package com.pfe.adminagent.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record RagSearchRequest(
        @NotBlank String query,
        Integer topK
) {
}
