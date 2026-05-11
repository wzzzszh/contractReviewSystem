package com.szh.contractReviewSystem.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class DeepSeekLLMService implements LLMService {

    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final DeepSeekThinkingState thinkingState;

    public DeepSeekLLMService(String apiKey, String model) {
        this(DEFAULT_BASE_URL, apiKey, model, null);
    }

    public DeepSeekLLMService(String baseUrl, String apiKey, String model) {
        this(baseUrl, apiKey, model, null);
    }

    public DeepSeekLLMService(String baseUrl, String apiKey, String model, DeepSeekThinkingState thinkingState) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.apiKey = apiKey;
        this.model = model;
        this.thinkingState = thinkingState;
    }

    @Override
    public String call(String prompt) throws Exception {
        return call(null, prompt);
    }

    @Override
    public String call(String systemPrompt, String userPrompt) throws Exception {
        try {
            JsonNode payload = buildPayload(systemPrompt, userPrompt);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(90))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw buildProviderException(response.statusCode(), response.body(), null);
            }

            String content = extractContent(response.body());
            if (content == null || content.trim().isEmpty()) {
                throw new LlmProviderException(
                        "deepseek",
                        "DeepSeek returned empty content",
                        response.statusCode(),
                        false,
                        false,
                        null
                );
            }
            return content;
        } catch (LlmProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmProviderException(
                    "deepseek",
                    "DeepSeek request failed: " + e.getMessage(),
                    null,
                    LlmErrorClassifier.isRetryable(e),
                    LlmErrorClassifier.isFallbackEligible(e),
                    e
            );
        }
    }

    private JsonNode buildPayload(String systemPrompt, String userPrompt) {
        var root = OBJECT_MAPPER.createObjectNode();
        root.put("model", model);
        var thinking = root.putObject("thinking");
        thinking.put("type", isThinkingEnabled() ? "enabled" : "disabled");
        var messages = root.putArray("messages");
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            messages.addObject()
                    .put("role", "system")
                    .put("content", systemPrompt.trim());
        }
        messages.addObject()
                .put("role", "user")
                .put("content", userPrompt == null ? "" : userPrompt.trim());
        return root;
    }

    private boolean isThinkingEnabled() {
        return thinkingState != null && thinkingState.isThinkingEnabled();
    }

    private String extractContent(String body) throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(body);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return null;
        }
        JsonNode message = choices.get(0).path("message");
        if (message.isMissingNode()) {
            return null;
        }
        return message.path("content").asText(null);
    }

    private LlmProviderException buildProviderException(int statusCode, String body, Throwable cause) {
        String message = "DeepSeek request failed with HTTP " + statusCode;
        boolean fallbackEligible = statusCode == 429 || statusCode == 402 || statusCode == 503;
        boolean retryable = fallbackEligible || statusCode >= 500;
        try {
            JsonNode root = OBJECT_MAPPER.readTree(body == null ? "{}" : body);
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                String apiMessage = error.path("message").asText(null);
                String apiCode = error.path("code").asText(null);
                if (apiMessage != null && !apiMessage.trim().isEmpty()) {
                    message = apiMessage.trim();
                }
                if (apiCode != null && !apiCode.trim().isEmpty()) {
                    message = message + " (" + apiCode.trim() + ")";
                }
                String normalized = (apiMessage == null ? "" : apiMessage).toLowerCase();
                normalized = normalized + " " + (apiCode == null ? "" : apiCode.toLowerCase());
                fallbackEligible = fallbackEligible
                        || normalized.contains("quota")
                        || normalized.contains("insufficient")
                        || normalized.contains("rate limit")
                        || normalized.contains("balance");
                retryable = retryable || normalized.contains("timeout") || normalized.contains("unavailable");
            } else if (body != null && !body.trim().isEmpty()) {
                message = message + ": " + body.trim();
            }
        } catch (Exception ignored) {
            if (body != null && !body.trim().isEmpty()) {
                message = message + ": " + body.trim();
            }
        }
        return new LlmProviderException("deepseek", message, statusCode, retryable, fallbackEligible, cause);
    }

    private String normalizeBaseUrl(String value) {
        String resolved = value == null || value.trim().isEmpty() ? DEFAULT_BASE_URL : value.trim();
        while (resolved.endsWith("/")) {
            resolved = resolved.substring(0, resolved.length() - 1);
        }
        return resolved;
    }
}
