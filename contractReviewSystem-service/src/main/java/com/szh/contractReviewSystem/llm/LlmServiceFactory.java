package com.szh.contractReviewSystem.llm;

import com.szh.contractReviewSystem.llm.ark.ArkLLMService;

public final class LlmServiceFactory {

    private LlmServiceFactory() {
    }

    public static LLMService tryCreateDefault() {
        LlmProvider requestedProvider = LlmProvider.from(firstNonBlank(
                System.getProperty("llm.provider"),
                System.getenv("LLM_PROVIDER")
        ));

        String arkApiKey = firstNonBlank(
                System.getProperty("ark.api-key"),
                System.getenv("ARK_API_KEY")
        );
        String arkModel = firstNonBlank(
                System.getProperty("ark.model"),
                System.getenv("ARK_MODEL")
        );
        String arkBaseUrl = firstNonBlank(
                System.getProperty("ark.base-url"),
                System.getenv("ARK_BASE_URL"),
                "https://ark.cn-beijing.volces.com/api/v3"
        );

        String deepSeekApiKey = firstNonBlank(
                System.getProperty("deepseek.api-key"),
                System.getenv("DEEPSEEK_API_KEY")
        );
        String deepSeekModel = firstNonBlank(
                System.getProperty("deepseek.model"),
                System.getenv("DEEPSEEK_MODEL"),
                "deepseek-v4-flash"
        );
        String deepSeekBaseUrl = firstNonBlank(
                System.getProperty("deepseek.base-url"),
                System.getenv("DEEPSEEK_BASE_URL"),
                "https://api.deepseek.com"
        );

        ArkLLMService arkService = isBlank(arkApiKey) || isBlank(arkModel)
                ? null
                : new ArkLLMService(arkBaseUrl, arkApiKey, arkModel);
        DeepSeekLLMService deepSeekService = isBlank(deepSeekApiKey) || isBlank(deepSeekModel)
                ? null
                : new DeepSeekLLMService(deepSeekBaseUrl, deepSeekApiKey, deepSeekModel);

        if (requestedProvider == LlmProvider.ARK) {
            return arkService == null ? deepSeekService : arkService;
        }
        if (requestedProvider == LlmProvider.DEEPSEEK) {
            return deepSeekService == null ? arkService : deepSeekService;
        }
        if (arkService != null && deepSeekService != null) {
            return new RoutingLLMService(
                    arkService,
                    deepSeekService,
                    new LlmProviderState(LlmProvider.ARK),
                    true
            );
        }
        return arkService == null ? deepSeekService : arkService;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
