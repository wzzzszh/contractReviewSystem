package com.szh.contractReviewSystem.controller.notdb;

import com.szh.contractReviewSystem.annotation.RequiresPermissions;
import com.szh.contractReviewSystem.common.Result;
import com.szh.contractReviewSystem.config.ArkConfig;
import com.szh.contractReviewSystem.config.DeepSeekConfig;
import com.szh.contractReviewSystem.config.LlmRoutingConfig;
import com.szh.contractReviewSystem.llm.ArkModelState;
import com.szh.contractReviewSystem.llm.DeepSeekThinkingState;
import com.szh.contractReviewSystem.llm.LlmProvider;
import com.szh.contractReviewSystem.llm.LlmProviderState;
import com.szh.contractReviewSystem.model.request.ArkModelSwitchRequest;
import com.szh.contractReviewSystem.model.request.DeepSeekThinkingSwitchRequest;
import com.szh.contractReviewSystem.model.request.LlmProviderSwitchRequest;
import com.szh.contractReviewSystem.model.response.LlmStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/llm")
public class LlmController extends BaseController {

    private final LlmProviderState providerState;
    private final LlmRoutingConfig routingConfig;
    private final ArkConfig arkConfig;
    private final DeepSeekConfig deepSeekConfig;
    private final DeepSeekThinkingState deepSeekThinkingState;
    private final ArkModelState arkModelState;

    public LlmController(LlmProviderState providerState,
                         LlmRoutingConfig routingConfig,
                         ArkConfig arkConfig,
                         DeepSeekConfig deepSeekConfig,
                         DeepSeekThinkingState deepSeekThinkingState,
                         ArkModelState arkModelState) {
        this.providerState = providerState;
        this.routingConfig = routingConfig;
        this.arkConfig = arkConfig;
        this.deepSeekConfig = deepSeekConfig;
        this.deepSeekThinkingState = deepSeekThinkingState;
        this.arkModelState = arkModelState;
    }

    @RequiresPermissions("llm:select")
    @GetMapping("/status")
    public Result<LlmStatusResponse> status() {
        return success(buildStatus("LLM provider status loaded"));
    }

    @RequiresPermissions("llm:select")
    @PostMapping("/provider")
    public Result<LlmStatusResponse> switchProvider(@Valid @RequestBody LlmProviderSwitchRequest request) {
        String provider = request.getProvider().trim();
        if ("auto".equalsIgnoreCase(provider)) {
            providerState.resetToPrimary();
            return success("LLM provider reset to configured primary", buildStatus("reset to configured primary"));
        }

        LlmProvider requestedProvider = LlmProvider.from(provider);
        if (requestedProvider == null) {
            return error(400, "provider must be one of: auto, ark, deepseek");
        }
        if (!isConfigured(requestedProvider)) {
            return error(400, requestedProvider.name().toLowerCase() + " provider is not configured");
        }

        providerState.setActiveProvider(requestedProvider);
        return success("LLM provider switched", buildStatus("switched to " + requestedProvider.name().toLowerCase()));
    }

    @RequiresPermissions("llm:select")
    @PostMapping("/deepseek-thinking")
    public Result<LlmStatusResponse> switchDeepSeekThinking(@Valid @RequestBody DeepSeekThinkingSwitchRequest request) {
        boolean enabled = Boolean.TRUE.equals(request.getEnabled());
        deepSeekThinkingState.setThinkingEnabled(enabled);
        String action = enabled ? "enabled" : "disabled";
        return success("DeepSeek thinking mode " + action, buildStatus("deepseek thinking " + action));
    }

    @RequiresPermissions("llm:select")
    @PostMapping("/ark-model")
    public Result<LlmStatusResponse> switchArkModel(@Valid @RequestBody ArkModelSwitchRequest request) {
        String model = request.getModel().trim();
        if (!arkModelState.setActiveModel(model)) {
            return error(400, "ark model is not available");
        }
        return success("Ark model switched", buildStatus("ark model switched"));
    }

    private LlmStatusResponse buildStatus(String message) {
        boolean arkConfigured = hasText(arkConfig == null ? null : arkConfig.getApiKey())
                && hasText(arkConfig == null ? null : arkConfig.getModel());
        boolean deepSeekConfigured = hasText(deepSeekConfig == null ? null : deepSeekConfig.getApiKey())
                && hasText(deepSeekConfig == null ? null : deepSeekConfig.getModel());

        LlmStatusResponse response = new LlmStatusResponse();
        response.setConfiguredMode(routingConfig == null ? "auto" : routingConfig.getProviderMode().name().toLowerCase());
        response.setPrimaryProvider(providerState.getPrimaryProvider().name().toLowerCase());
        response.setActiveProvider(providerState.getActiveProvider().name().toLowerCase());
        response.setStickyFallback(routingConfig == null || routingConfig.isStickyFallback());
        response.setArkConfigured(arkConfigured);
        response.setDeepSeekConfigured(deepSeekConfigured);
        response.setAvailableProviders(availableProviders(arkConfigured, deepSeekConfigured));
        response.setArkModel(arkModelState == null ? (arkConfig == null ? null : arkConfig.getModel()) : arkModelState.getActiveModel());
        response.setArkModels(arkModelState == null ? List.of() : arkModelState.getAvailableModels());
        response.setDeepSeekModel(deepSeekConfig == null ? null : deepSeekConfig.getModel());
        response.setDeepSeekThinkingEnabled(deepSeekThinkingState.isThinkingEnabled());
        response.setMessage(message);
        return response;
    }

    private List<String> availableProviders(boolean arkConfigured, boolean deepSeekConfigured) {
        List<String> providers = new ArrayList<>();
        if (arkConfigured) {
            providers.add("ark");
        }
        if (deepSeekConfigured) {
            providers.add("deepseek");
        }
        return providers;
    }

    private boolean isConfigured(LlmProvider provider) {
        if (provider == LlmProvider.DEEPSEEK) {
            return hasText(deepSeekConfig == null ? null : deepSeekConfig.getApiKey())
                    && hasText(deepSeekConfig == null ? null : deepSeekConfig.getModel());
        }
        return hasText(arkConfig == null ? null : arkConfig.getApiKey())
                && hasText(arkConfig == null ? null : arkConfig.getModel());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
