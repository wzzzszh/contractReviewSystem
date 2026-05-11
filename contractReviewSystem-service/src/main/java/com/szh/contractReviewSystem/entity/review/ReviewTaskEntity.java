package com.szh.contractReviewSystem.entity.review;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewTaskEntity {

    private Long id;

    private Long userId;

    private Long sourceFileId;

    private Long resultFileId;

    private String taskType;

    private String status;

    private Integer progress;

    private String perspective;

    private String userFocus;

    private String riskReport;

    private String generatedRequirement;

    private Integer appliedOperationCount;

    private Integer skippedOperationCount;

    private String skippedOperationMessagesJson;

    private List<String> skippedOperationMessages;

    private Boolean retryable;

    private Integer retryCount;

    private Integer maxRetry;

    private String lastErrorCode;

    private String errorMessage;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
