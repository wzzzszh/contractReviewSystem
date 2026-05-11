package com.szh.contractReviewSystem.llm;

import java.util.Locale;

public final class LlmErrorClassifier {

    private LlmErrorClassifier() {
    }

    public static boolean isFallbackEligible(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("insufficient_quota")
                        || normalized.contains("insufficient balance")
                        || normalized.contains("insufficient_balance")
                        || normalized.contains("quota")
                        || normalized.contains("rate limit")
                        || normalized.contains("ratelimit")
                        || normalized.contains("too many requests")
                        || normalized.contains("exceed")
                        || normalized.contains("billing")
                        || normalized.contains("余额")
                        || normalized.contains("额度")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    public static boolean isRetryable(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("timeout")
                        || normalized.contains("connect")
                        || normalized.contains("socket")
                        || normalized.contains("temporary")
                        || normalized.contains("unavailable")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return isFallbackEligible(throwable);
    }
}
