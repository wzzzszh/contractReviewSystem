package com.szh.contractReviewSystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekConfig {

    private String apiKey;
    private String baseUrl = "https://api.deepseek.com";
    private String model = "deepseek-v4-flash";
    private boolean thinkingEnabled = false;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isThinkingEnabled() {
        return thinkingEnabled;
    }

    public void setThinkingEnabled(boolean thinkingEnabled) {
        this.thinkingEnabled = thinkingEnabled;
    }
}
