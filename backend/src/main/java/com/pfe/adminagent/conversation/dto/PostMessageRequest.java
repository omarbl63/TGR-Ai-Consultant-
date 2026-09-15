package com.pfe.adminagent.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostMessageRequest(
        @NotBlank @Size(max = 8000) String content
) {
}
