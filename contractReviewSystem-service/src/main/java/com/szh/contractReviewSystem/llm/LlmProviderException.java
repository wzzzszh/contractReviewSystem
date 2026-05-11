package com.szh.contractReviewSystem.llm;

public class LlmProviderException extends RuntimeException {

    private final String provider;
    private final Integer statusCode;
    private final boolean retryable;
    private final boolean fallbackEligible;

    public LlmProviderException(String provider,
                                String message,
                                Integer statusCode,
                                boolean retryable,
                                boolean fallbackEligible,
                                Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.statusCode = statusCode;
        this.retryable = retryable;
        this.fallbackEligible = fallbackEligible;
    }

    public String getProvider() {
        return provider;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public boolean isFallbackEligible() {
        return fallbackEligible;
    }
}
