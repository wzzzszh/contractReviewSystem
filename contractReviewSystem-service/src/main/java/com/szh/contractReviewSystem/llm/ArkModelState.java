package com.szh.contractReviewSystem.llm;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ArkModelState {

    private final String defaultModel;
    private final List<String> availableModels;
    private final AtomicReference<String> activeModel;

    public ArkModelState(String defaultModel, List<String> availableModels) {
        this.defaultModel = normalize(defaultModel);
        this.availableModels = availableModels == null
                ? List.of()
                : availableModels.stream()
                .map(this::normalize)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        this.activeModel = new AtomicReference<>(this.defaultModel);
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public List<String> getAvailableModels() {
        return availableModels;
    }

    public String getActiveModel() {
        String model = activeModel.get();
        return model == null || model.isBlank() ? defaultModel : model;
    }

    public boolean setActiveModel(String model) {
        String normalized = normalize(model);
        if (normalized == null || normalized.isBlank() || !availableModels.contains(normalized)) {
            return false;
        }
        activeModel.set(normalized);
        return true;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
