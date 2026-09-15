package com.pfe.adminagent.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskRequest(
        @NotBlank @Size(max = 2000) String question,
        Integer topK
) {
}
