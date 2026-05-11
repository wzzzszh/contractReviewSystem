package com.szh.contractReviewSystem.llm;

import java.util.concurrent.atomic.AtomicBoolean;

public class DeepSeekThinkingState {

    private final AtomicBoolean thinkingEnabled;

    public DeepSeekThinkingState(boolean enabled) {
        this.thinkingEnabled = new AtomicBoolean(enabled);
    }

    public boolean isThinkingEnabled() {
        return thinkingEnabled.get();
    }

    public void setThinkingEnabled(boolean enabled) {
        thinkingEnabled.set(enabled);
    }
}
