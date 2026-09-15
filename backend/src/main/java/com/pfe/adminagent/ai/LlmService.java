package com.pfe.adminagent.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Thin wrapper over the LangChain4j {@link ChatLanguageModel} providing free-text
 * completion and tolerant JSON completion (models sometimes wrap JSON in prose or
 * ```json fences — we extract the first balanced object).
 */
@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    private final ChatLanguageModel model;
    private final ObjectMapper objectMapper;

    public LlmService(ChatLanguageModel model, ObjectMapper objectMapper) {
        this.model = model;
        this.objectMapper = objectMapper;
    }

    public String complete(String systemPrompt, String userPrompt) {
        List<ChatMessage> messages = List.of(SystemMessage.from(systemPrompt), UserMessage.from(userPrompt));
        Response<AiMessage> response = model.generate(messages);
        return response.content().text().trim();
    }

    /**
     * Completes and parses the response into {@code type}. Throws if no JSON object
     * can be extracted or parsed.
     */
    public <T> T completeJson(String systemPrompt, String userPrompt, Class<T> type) {
        String raw = complete(systemPrompt, userPrompt);
        String json = extractJsonObject(raw);
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("Failed to parse LLM JSON into {}: {}\nRaw: {}", type.getSimpleName(), e.getMessage(), raw);
            throw new LlmParsingException("The AI returned an unparseable response", e);
        }
    }

    /** Extracts the first balanced {@code {...}} block, ignoring markdown fences/prose. */
    static String extractJsonObject(String text) {
        if (text == null) {
            throw new LlmParsingException("Empty AI response", null);
        }
        int start = text.indexOf('{');
        if (start < 0) {
            throw new LlmParsingException("No JSON object in AI response", null);
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        throw new LlmParsingException("Unbalanced JSON in AI response", null);
    }

    public static class LlmParsingException extends RuntimeException {
        public LlmParsingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
