package com.szh.contractReviewSystem.llm.ark;

import com.szh.contractReviewSystem.llm.LLMService;
import com.szh.contractReviewSystem.llm.ArkModelState;
import com.szh.contractReviewSystem.llm.LlmErrorClassifier;
import com.szh.contractReviewSystem.llm.LlmProviderException;

import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ArkLLMService implements LLMService {

    private static final String DEFAULT_BASE_URL = "https://ark.cn-beijing.volces.com/api/v3";

    private final ArkService arkService;
    private final String baseUrl;
    private final String model;
    private final ArkModelState modelState;

    public ArkLLMService(String apiKey, String model) {
        this(DEFAULT_BASE_URL, apiKey, model, null);
    }

    public ArkLLMService(String baseUrl, String apiKey, String model) {
        this(baseUrl, apiKey, model, null);
    }

    public ArkLLMService(String baseUrl, String apiKey, String model, ArkModelState modelState) {
        ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
        Dispatcher dispatcher = new Dispatcher();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.arkService = ArkService.builder()
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .baseUrl(this.baseUrl)
                .apiKey(apiKey)
                .build();
        this.model = model;
        this.modelState = modelState;
    }

    @Override
    public String call(String prompt) throws Exception {
        return call(null, prompt);
    }

    @Override
    public String call(String systemPrompt, String userPrompt) throws Exception {
        final List<ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content(systemPrompt.trim())
                    .build());
        }
        messages.add(ChatMessage.builder()
                .role(ChatMessageRole.USER)
                .content(userPrompt == null ? "" : userPrompt.trim())
                .build());
        
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(activeModel())
                .messages(messages)
                .build();

        try {
            StringBuilder result = new StringBuilder();
            arkService.createChatCompletion(chatCompletionRequest)
                    .getChoices()
                    .forEach(choice -> {
                        if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                            result.append(choice.getMessage().getContent());
                        }
                    });

            String content = result.toString();
            if (content.trim().isEmpty()) {
                throw new IllegalStateException("Ark returned empty content");
            }
            return content;
        } catch (LlmProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmProviderException(
                    "ark",
                    "Ark request failed: " + e.getMessage(),
                    null,
                    LlmErrorClassifier.isRetryable(e),
                    LlmErrorClassifier.isFallbackEligible(e),
                    e
            );
        }
    }

    public void shutdown() {
        arkService.shutdownExecutor();
    }

    private String normalizeBaseUrl(String value) {
        String resolved = value == null || value.trim().isEmpty() ? DEFAULT_BASE_URL : value.trim();
        while (resolved.endsWith("/")) {
            resolved = resolved.substring(0, resolved.length() - 1);
        }
        return resolved;
    }

    private String activeModel() {
        return modelState == null || modelState.getActiveModel() == null
                ? model
                : modelState.getActiveModel();
    }
}
