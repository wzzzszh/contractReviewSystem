package com.szh.contractReviewSystem.llm;

import java.util.concurrent.atomic.AtomicReference;

public class LlmProviderState {

    private final LlmProvider primaryProvider;
    private final AtomicReference<LlmProvider> activeProvider;

    public LlmProviderState(LlmProvider initialProvider) {
        this.primaryProvider = initialProvider == null ? LlmProvider.ARK : initialProvider;
        this.activeProvider = new AtomicReference<>(this.primaryProvider);
    }

    public LlmProvider getPrimaryProvider() {
        return primaryProvider;
    }

    public LlmProvider getActiveProvider() {
        return activeProvider.get();
    }

    public void setActiveProvider(LlmProvider provider) {
        if (provider != null) {
            activeProvider.set(provider);
        }
    }

    public void resetToPrimary() {
        activeProvider.set(primaryProvider);
    }
}
