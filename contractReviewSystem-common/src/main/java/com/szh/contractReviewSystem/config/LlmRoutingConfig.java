package com.szh.contractReviewSystem.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmRoutingConfig {

    private String provider = "auto";
    private boolean stickyFallback = true;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isStickyFallback() {
        return stickyFallback;
    }

    public void setStickyFallback(boolean stickyFallback) {
        this.stickyFallback = stickyFallback;
    }

    public ProviderMode getProviderMode() {
        if (provider == null || provider.trim().isEmpty()) {
            return ProviderMode.AUTO;
        }
        return ProviderMode.from(provider);
    }

    public enum ProviderMode {
        AUTO,
        ARK,
        DEEPSEEK;

        public static ProviderMode from(String value) {
            if (value == null) {
                return AUTO;
            }
            String normalized = value.trim().toLowerCase();
            if ("ark".equals(normalized)) {
                return ARK;
            }
            if ("deepseek".equals(normalized)) {
                return DEEPSEEK;
            }
            return AUTO;
        }
    }
}
