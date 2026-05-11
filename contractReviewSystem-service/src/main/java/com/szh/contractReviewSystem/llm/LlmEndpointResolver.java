package com.szh.contractReviewSystem.llm;

import com.szh.contractReviewSystem.agent.docx.DocxSkillAgentProperties;
import com.szh.contractReviewSystem.config.ArkConfig;
import com.szh.contractReviewSystem.config.DeepSeekConfig;
import com.szh.contractReviewSystem.config.LlmRoutingConfig;

public class LlmEndpointResolver {

    private final LlmRoutingConfig routingConfig;
    private final ArkConfig arkConfig;
    private final DeepSeekConfig deepSeekConfig;
    private final DocxSkillAgentProperties docxSkillAgentProperties;
    private final LlmProviderState providerState;

    public LlmEndpointResolver(LlmRoutingConfig routingConfig,
                               ArkConfig arkConfig,
                               DeepSeekConfig deepSeekConfig,
                               DocxSkillAgentProperties docxSkillAgentProperties,
                               LlmProviderState providerState) {
        this.routingConfig = routingConfig;
        this.arkConfig = arkConfig;
        this.deepSeekConfig = deepSeekConfig;
        this.docxSkillAgentProperties = docxSkillAgentProperties;
        this.providerState = providerState;
    }

    public ResolvedEndpoint resolveCurrent() {
        return resolve(providerState == null ? null : providerState.getActiveProvider(), null, null, null);
    }

    public ResolvedEndpoint resolveDocxAgent() {
        return resolveCurrent();
    }

    public ResolvedEndpoint resolve(LlmProvider requestedProvider,
                                    String apiKeyOverride,
                                    String baseUrlOverride,
                                    String modelOverride) {
        LlmProvider provider = requestedProvider;
        if (provider == null) {
            provider = routingConfig == null ? LlmProvider.ARK : routingConfig.getProviderMode() == LlmRoutingConfig.ProviderMode.DEEPSEEK
                    ? LlmProvider.DEEPSEEK
                    : LlmProvider.ARK;
        }

        ResolvedEndpoint endpoint = provider == LlmProvider.DEEPSEEK
                ? resolveDeepSeek(apiKeyOverride, baseUrlOverride, modelOverride)
                : resolveArk(apiKeyOverride, baseUrlOverride, modelOverride);

        if (!endpoint.isConfigured()) {
            ResolvedEndpoint fallback = provider == LlmProvider.DEEPSEEK
                    ? resolveArk(null, null, null)
                    : resolveDeepSeek(null, null, null);
            if (fallback.isConfigured()) {
                return fallback;
            }
        }
        return endpoint;
    }

    private ResolvedEndpoint resolveArk(String apiKeyOverride, String baseUrlOverride, String modelOverride) {
        return new ResolvedEndpoint(
                LlmProvider.ARK,
                firstNonBlank(apiKeyOverride, arkConfig == null ? null : arkConfig.getApiKey()),
                firstNonBlank(baseUrlOverride, arkConfig == null ? null : arkConfig.getBaseUrl(), "https://ark.cn-beijing.volces.com/api/v3"),
                firstNonBlank(modelOverride, arkConfig == null ? null : arkConfig.getModel())
        );
    }

    private ResolvedEndpoint resolveDeepSeek(String apiKeyOverride, String baseUrlOverride, String modelOverride) {
        return new ResolvedEndpoint(
                LlmProvider.DEEPSEEK,
                firstNonBlank(apiKeyOverride, deepSeekConfig == null ? null : deepSeekConfig.getApiKey()),
                firstNonBlank(baseUrlOverride, deepSeekConfig == null ? null : deepSeekConfig.getBaseUrl(), "https://api.deepseek.com"),
                firstNonBlank(modelOverride, deepSeekConfig == null ? null : deepSeekConfig.getModel(), "deepseek-v4-flash")
        );
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    public static class ResolvedEndpoint {
        private final LlmProvider provider;
        private final String apiKey;
        private final String baseUrl;
        private final String model;

        public ResolvedEndpoint(LlmProvider provider, String apiKey, String baseUrl, String model) {
            this.provider = provider;
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
            this.model = model;
        }

        public LlmProvider getProvider() {
            return provider;
        }

        public String getApiKey() {
            return apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public String getModel() {
            return model;
        }

        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank() && model != null && !model.isBlank();
        }
    }
}
