package com.pfe.adminagent.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmIntent(
        String intent,
        Double confidence
) {
}
