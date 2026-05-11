package com.szh.contractReviewSystem.model.response;

import lombok.Data;

import java.util.List;

@Data
public class LlmStatusResponse {

    private String configuredMode;

    private String primaryProvider;

    private String activeProvider;

    private boolean stickyFallback;

    private boolean arkConfigured;

    private boolean deepSeekConfigured;

    private List<String> availableProviders;

    private String arkModel;

    private List<String> arkModels;

    private String deepSeekModel;

    private boolean deepSeekThinkingEnabled;

    private String message;
}
