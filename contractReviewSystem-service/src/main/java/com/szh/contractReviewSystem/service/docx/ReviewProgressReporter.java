package com.szh.contractReviewSystem.service.docx;

@FunctionalInterface
public interface ReviewProgressReporter {

    ReviewProgressReporter NOOP = progress -> {
    };

    void updateProgress(int progress);
}
