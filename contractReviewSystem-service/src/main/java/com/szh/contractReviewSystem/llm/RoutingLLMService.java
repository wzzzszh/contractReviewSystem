package com.szh.contractReviewSystem.llm;

import com.szh.contractReviewSystem.llm.ark.ArkLLMService;

public class RoutingLLMService implements LLMService {

    private final ArkLLMService arkService;
    private final DeepSeekLLMService deepSeekService;
    private final LlmProviderState providerState;
    private final boolean stickyFallback;

    public RoutingLLMService(ArkLLMService arkService,
                             DeepSeekLLMService deepSeekService,
                             LlmProviderState providerState,
                             boolean stickyFallback) {
        this.arkService = arkService;
        this.deepSeekService = deepSeekService;
        this.providerState = providerState == null ? new LlmProviderState(LlmProvider.ARK) : providerState;
        this.stickyFallback = stickyFallback;
    }

    @Override
    public String call(String prompt) throws Exception {
        return call(null, prompt);
    }

    @Override
    public String call(String systemPrompt, String userPrompt) throws Exception {
        LlmProvider active = providerState.getActiveProvider();
        if (active == LlmProvider.DEEPSEEK) {
            if (deepSeekService == null && arkService != null) {
                providerState.setActiveProvider(LlmProvider.ARK);
                return arkService.call(systemPrompt, userPrompt);
            }
            return callDeepSeek(systemPrompt, userPrompt);
        }

        if (arkService == null && deepSeekService != null) {
            providerState.setActiveProvider(LlmProvider.DEEPSEEK);
            return callDeepSeek(systemPrompt, userPrompt);
        }
        if (arkService == null) {
            if (deepSeekService != null) {
                providerState.setActiveProvider(LlmProvider.DEEPSEEK);
                return callDeepSeek(systemPrompt, userPrompt);
            }
            throw new IllegalStateException("No LLM provider is configured");
        }

        try {
            return arkService.call(systemPrompt, userPrompt);
        } catch (LlmProviderException e) {
            if (shouldFallback(e)) {
                providerState.setActiveProvider(LlmProvider.DEEPSEEK);
                return callDeepSeek(systemPrompt, userPrompt);
            }
            throw e;
        } catch (Exception e) {
            if (shouldFallback(e)) {
                providerState.setActiveProvider(LlmProvider.DEEPSEEK);
                return callDeepSeek(systemPrompt, userPrompt);
            }
            throw e;
        }
    }

    @Override
    public void shutdown() {
        if (arkService != null) {
            arkService.shutdown();
        }
        if (deepSeekService != null) {
            deepSeekService.shutdown();
        }
    }

    private String callDeepSeek(String systemPrompt, String userPrompt) throws Exception {
        if (deepSeekService == null) {
            throw new IllegalStateException("DeepSeek provider is not configured");
        }
        try {
            return deepSeekService.call(systemPrompt, userPrompt);
        } catch (LlmProviderException e) {
            if (shouldFallback(e) && providerState.getActiveProvider() != LlmProvider.DEEPSEEK) {
                providerState.setActiveProvider(LlmProvider.DEEPSEEK);
            }
            throw e;
        }
    }

    private boolean shouldFallback(Exception e) {
        return stickyFallback && LlmErrorClassifier.isFallbackEligible(e);
    }
}
