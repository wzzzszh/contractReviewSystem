package com.szh.contractReviewSystem.llm;

import java.util.Locale;

public enum LlmProvider {
    ARK,
    DEEPSEEK;

    public static LlmProvider from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("DEEPSEEK".equals(normalized) || "DEEPSEEK_V4".equals(normalized)) {
            return DEEPSEEK;
        }
        if ("ARK".equals(normalized) || "VOLCENGINE".equals(normalized)) {
            return ARK;
        }
        return null;
    }
}
