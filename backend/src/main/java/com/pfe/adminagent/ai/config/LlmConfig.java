package com.pfe.adminagent.ai.config;

import com.pfe.adminagent.config.AiProperties;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

/**
 * Configures the LLM (Qwen3) via an OpenAI-compatible endpoint (OpenRouter/DashScope).
 * The model is exposed as a LangChain4j {@link ChatLanguageModel} abstraction so the
 * orchestrator never talks to a raw HTTP API.
 */
@Configuration
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    @Bean
    public ChatLanguageModel chatLanguageModel(AiProperties props) {
        AiProperties.Llm llm = props.getLlm();
        log.info("Configuring LLM '{}' at {}", llm.getModel(), llm.getBaseUrl());
        return OpenAiChatModel.builder()
                .baseUrl(llm.getBaseUrl())
                .apiKey(llm.getApiKey())
                .modelName(llm.getModel())
                .temperature(llm.getTemperature())
                .maxTokens(llm.getMaxTokens())
                .timeout(Duration.ofSeconds(120))
                .maxRetries(2)
                // OpenRouter attribution headers (optional but recommended).
                .customHeaders(Map.of(
                        "HTTP-Referer", "https://adminai.pfe.local",
                        "X-Title", "AdminAI"))
                .build();
    }
}
