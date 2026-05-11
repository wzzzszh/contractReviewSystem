package com.szh.contractReviewSystem.llm;

public interface LLMService {

    String call(String prompt) throws Exception;

    default String call(String systemPrompt, String userPrompt) throws Exception {
        String system = systemPrompt == null ? "" : systemPrompt.trim();
        String user = userPrompt == null ? "" : userPrompt.trim();
        if (system.isEmpty()) {
            return call(user);
        }
        if (user.isEmpty()) {
            return call(system);
        }
        return call(system + "\n\n" + user);
    }

    default void shutdown() {
    }
}
