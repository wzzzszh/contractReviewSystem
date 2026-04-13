package com.szh.contractReviewSystem.llm;

import com.szh.contractReviewSystem.llm.ark.ArkLLMService;

public final class LlmServiceFactory {

    private LlmServiceFactory() {
    }

    public static LLMService tryCreateDefault() {
        String apiKey = firstNonBlank(
                System.getProperty("ark.api-key"),
                System.getenv("ARK_API_KEY")
        );
        String model = firstNonBlank(
                System.getProperty("ark.model"),
                System.getenv("ARK_MODEL")
        );

        if (isBlank(apiKey) || isBlank(model)) {
            return null;
        }
        return new ArkLLMService(apiKey, model);
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
