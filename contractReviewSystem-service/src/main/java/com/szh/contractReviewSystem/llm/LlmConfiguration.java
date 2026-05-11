package com.szh.contractReviewSystem.llm;

import com.szh.contractReviewSystem.agent.docx.DocxSkillAgentProperties;
import com.szh.contractReviewSystem.config.ArkConfig;
import com.szh.contractReviewSystem.config.DeepSeekConfig;
import com.szh.contractReviewSystem.config.LlmRoutingConfig;
import com.szh.contractReviewSystem.llm.ark.ArkLLMService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class LlmConfiguration {

    @Bean
    public LlmProviderState llmProviderState(LlmRoutingConfig llmRoutingConfig,
                                             ArkConfig arkConfig,
                                             DeepSeekConfig deepSeekConfig) {
        return new LlmProviderState(resolveInitialProvider(llmRoutingConfig, arkConfig, deepSeekConfig));
    }

    @Bean
    public DeepSeekThinkingState deepSeekThinkingState(DeepSeekConfig deepSeekConfig) {
        return new DeepSeekThinkingState(deepSeekConfig != null && deepSeekConfig.isThinkingEnabled());
    }

    @Bean
    public ArkModelState arkModelState(ArkConfig arkConfig) {
        return new ArkModelState(resolveArkDefaultModel(arkConfig), resolveArkModels(arkConfig));
    }

    @Bean
    public LLMService llmService(LlmRoutingConfig llmRoutingConfig,
                                 ArkConfig arkConfig,
                                 DeepSeekConfig deepSeekConfig,
                                 LlmProviderState llmProviderState,
                                 DeepSeekThinkingState deepSeekThinkingState,
                                 ArkModelState arkModelState) {
        return buildLlmService(llmRoutingConfig, arkConfig, deepSeekConfig, llmProviderState, deepSeekThinkingState, arkModelState);
    }

    @Bean
    public LlmEndpointResolver llmEndpointResolver(LlmRoutingConfig llmRoutingConfig,
                                                   ArkConfig arkConfig,
                                                   DeepSeekConfig deepSeekConfig,
                                                   DocxSkillAgentProperties docxSkillAgentProperties,
                                                   LlmProviderState llmProviderState) {
        return new LlmEndpointResolver(llmRoutingConfig, arkConfig, deepSeekConfig, docxSkillAgentProperties, llmProviderState);
    }

    static LLMService buildLlmService(LlmRoutingConfig routingConfig,
                                      ArkConfig arkConfig,
                                      DeepSeekConfig deepSeekConfig,
                                      LlmProviderState providerState,
                                      DeepSeekThinkingState deepSeekThinkingState,
                                      ArkModelState arkModelState) {
        ArkLLMService arkService = createArkService(arkConfig, arkModelState);
        DeepSeekLLMService deepSeekService = createDeepSeekService(deepSeekConfig, deepSeekThinkingState);
        LlmProviderState state = providerState == null
                ? new LlmProviderState(resolveInitialProvider(routingConfig, arkConfig, deepSeekConfig))
                : providerState;

        if (arkService != null && deepSeekService != null) {
            return new RoutingLLMService(arkService, deepSeekService, state,
                    routingConfig == null || routingConfig.isStickyFallback());
        }
        if (arkService != null) {
            state.setActiveProvider(LlmProvider.ARK);
            return arkService;
        }
        if (deepSeekService != null) {
            state.setActiveProvider(LlmProvider.DEEPSEEK);
            return deepSeekService;
        }
        return new LLMService() {
            @Override
            public String call(String prompt) {
                throw new IllegalStateException("No LLM provider configured");
            }
        };
    }

    private static LlmProvider resolveInitialProvider(LlmRoutingConfig routingConfig,
                                                      ArkConfig arkConfig,
                                                      DeepSeekConfig deepSeekConfig) {
        LlmRoutingConfig.ProviderMode mode = routingConfig == null ? LlmRoutingConfig.ProviderMode.AUTO : routingConfig.getProviderMode();
        switch (mode) {
            case ARK:
                return hasArkConfig(arkConfig) ? LlmProvider.ARK : (hasDeepSeekConfig(deepSeekConfig) ? LlmProvider.DEEPSEEK : LlmProvider.ARK);
            case DEEPSEEK:
                return hasDeepSeekConfig(deepSeekConfig) ? LlmProvider.DEEPSEEK : (hasArkConfig(arkConfig) ? LlmProvider.ARK : LlmProvider.DEEPSEEK);
            default:
                return hasArkConfig(arkConfig) ? LlmProvider.ARK : LlmProvider.DEEPSEEK;
        }
    }

    private static ArkLLMService createArkService(ArkConfig arkConfig, ArkModelState arkModelState) {
        if (!hasArkConfig(arkConfig)) {
            return null;
        }
        return new ArkLLMService(arkConfig.getBaseUrl(), arkConfig.getApiKey(), arkConfig.getModel(), arkModelState);
    }

    private static DeepSeekLLMService createDeepSeekService(DeepSeekConfig deepSeekConfig,
                                                            DeepSeekThinkingState deepSeekThinkingState) {
        if (!hasDeepSeekConfig(deepSeekConfig)) {
            return null;
        }
        return new DeepSeekLLMService(
                deepSeekConfig.getBaseUrl(),
                deepSeekConfig.getApiKey(),
                deepSeekConfig.getModel(),
                deepSeekThinkingState
        );
    }

    private static boolean hasArkConfig(ArkConfig arkConfig) {
        return arkConfig != null
                && notBlank(arkConfig.getApiKey())
                && notBlank(arkConfig.getModel());
    }

    private static boolean hasDeepSeekConfig(DeepSeekConfig deepSeekConfig) {
        return deepSeekConfig != null
                && notBlank(deepSeekConfig.getApiKey())
                && notBlank(deepSeekConfig.getModel());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String resolveArkDefaultModel(ArkConfig arkConfig) {
        return arkConfig == null ? null : arkConfig.getModel();
    }

    private static List<String> resolveArkModels(ArkConfig arkConfig) {
        List<String> models = new ArrayList<>();
        if (arkConfig != null && notBlank(arkConfig.getModel())) {
            models.add(arkConfig.getModel().trim());
        }
        if (arkConfig != null && arkConfig.getModels() != null) {
            arkConfig.getModels().stream()
                    .filter(LlmConfiguration::notBlank)
                    .map(String::trim)
                    .forEach(models::add);
        }
        return models.stream().distinct().toList();
    }
}
