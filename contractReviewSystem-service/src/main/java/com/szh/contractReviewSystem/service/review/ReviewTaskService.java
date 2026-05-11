package com.szh.contractReviewSystem.service.review;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.szh.contractReviewSystem.agent.docx.model.DocxModifyPerspective;
import com.szh.contractReviewSystem.entity.file.FileStorageEntity;
import com.szh.contractReviewSystem.entity.review.ReviewTaskEntity;
import com.szh.contractReviewSystem.exception.BusinessExceptionEnum;
import com.szh.contractReviewSystem.exception.CustomException;
import com.szh.contractReviewSystem.mapper.review.ReviewTaskMapper;
import com.szh.contractReviewSystem.model.request.CreateReviewTaskRequest;
import com.szh.contractReviewSystem.model.response.ReviewTaskResponse;
import com.szh.contractReviewSystem.service.db.FileStorageRecordService;
import com.szh.contractReviewSystem.utils.UserContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class ReviewTaskService {

    public static final String TASK_TYPE_DOCX_REVIEW_MODIFY = "docx_review_modify";
    public static final String STATUS_PENDING = "pending";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final ReviewTaskMapper reviewTaskMapper;
    private final FileStorageRecordService fileStorageRecordService;
    private final ReviewTaskExecutor reviewTaskExecutor;

    public ReviewTaskService(ReviewTaskMapper reviewTaskMapper,
                             FileStorageRecordService fileStorageRecordService,
                             ReviewTaskExecutor reviewTaskExecutor) {
        this.reviewTaskMapper = reviewTaskMapper;
        this.fileStorageRecordService = fileStorageRecordService;
        this.reviewTaskExecutor = reviewTaskExecutor;
    }

    public ReviewTaskResponse createTask(CreateReviewTaskRequest request) {
        if (request == null) {
            throw new CustomException(BusinessExceptionEnum.PARAMETER_ERROR, "request must not be null");
        }

        Long userId = UserContextHolder.requireUserId();
        FileStorageEntity sourceFile =
                fileStorageRecordService.getOwnedActiveRecord(request.getSourceFileId(), userId);
        requireDocxFile(sourceFile);

        ReviewTaskEntity task = new ReviewTaskEntity();
        task.setUserId(userId);
        task.setSourceFileId(sourceFile.getId());
        task.setTaskType(normalizeTaskType(request.getTaskType()));
        task.setStatus(STATUS_PENDING);
        task.setProgress(0);
        task.setPerspective(DocxModifyPerspective.resolveOrDefault(request.getPerspective()).name());
        task.setUserFocus(firstNonBlank(request.getUserFocus(), request.getModificationRequirement()));
        task.setRetryable(Boolean.FALSE);
        task.setRetryCount(0);
        task.setMaxRetry(3);
        task.setSkippedOperationMessagesJson("[]");
        reviewTaskMapper.insert(task);

        submitTaskExecution(task.getId());
        return toResponse(reviewTaskMapper.selectById(task.getId()));
    }

    public ReviewTaskResponse getOwnedTask(Long id) {
        Long userId = UserContextHolder.requireUserId();
        ReviewTaskEntity task = reviewTaskMapper.selectOwnedById(id, userId);
        if (task == null) {
            throw new CustomException(BusinessExceptionEnum.DATA_NOT_FOUND, "review task not found");
        }
        return toResponse(task);
    }

    public List<ReviewTaskResponse> listOwnedTasks() {
        Long userId = UserContextHolder.requireUserId();
        return reviewTaskMapper.selectByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ReviewTaskResponse retryOwnedTask(Long id) {
        Long userId = UserContextHolder.requireUserId();
        ReviewTaskEntity task = reviewTaskMapper.selectOwnedById(id, userId);
        if (task == null) {
            throw new CustomException(BusinessExceptionEnum.DATA_NOT_FOUND, "review task not found");
        }
        if (!Boolean.TRUE.equals(task.getRetryable())
                || !"failed".equalsIgnoreCase(task.getStatus())
                || safeInt(task.getRetryCount()) >= safeInt(task.getMaxRetry(), 3)) {
            throw new CustomException(BusinessExceptionEnum.REVIEW_PARAMETER_ERROR, "task is not retryable");
        }

        int updated = reviewTaskMapper.retryTaskByOwner(id, userId);
        if (updated == 0) {
            throw new CustomException(BusinessExceptionEnum.REVIEW_PARAMETER_ERROR,
                    "task is not retryable or has already been handled");
        }

        submitTaskExecution(id);
        return toResponse(reviewTaskMapper.selectOwnedById(id, userId));
    }

    private void submitTaskExecution(Long taskId) {
        try {
            reviewTaskExecutor.executeReviewTask(taskId);
        } catch (RuntimeException e) {
            reviewTaskMapper.markFailed(taskId, "review task queue rejected submission", true,
                    "QUEUE_REJECTED", null, null, null, null, null);
            throw new CustomException(BusinessExceptionEnum.REVIEW_FAILED,
                    "review task queue rejected submission", e);
        }
    }

    private String normalizeTaskType(String taskType) {
        String normalized = taskType == null || taskType.trim().isEmpty()
                ? TASK_TYPE_DOCX_REVIEW_MODIFY
                : taskType.trim().toLowerCase(Locale.ROOT);
        if (!TASK_TYPE_DOCX_REVIEW_MODIFY.equals(normalized)) {
            throw new CustomException(BusinessExceptionEnum.REVIEW_PARAMETER_ERROR,
                    "unsupported review task type: " + taskType);
        }
        return normalized;
    }

    private void requireDocxFile(FileStorageEntity sourceFile) {
        String filePath = sourceFile == null ? null : sourceFile.getFilePath();
        if (filePath == null || !filePath.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw new CustomException(BusinessExceptionEnum.REVIEW_PARAMETER_ERROR,
                    "source file must be a docx file");
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        if (second != null && !second.trim().isEmpty()) {
            return second.trim();
        }
        return null;
    }

    private ReviewTaskResponse toResponse(ReviewTaskEntity task) {
        if (task == null) {
            return null;
        }
        task.setSkippedOperationMessages(parseJsonList(task.getSkippedOperationMessagesJson()));

        ReviewTaskResponse response = new ReviewTaskResponse();
        response.setId(task.getId());
        response.setSourceFileId(task.getSourceFileId());
        response.setResultFileId(task.getResultFileId());
        response.setTaskType(task.getTaskType());
        response.setStatus(task.getStatus());
        response.setProgress(task.getProgress());
        response.setPerspective(task.getPerspective());
        response.setUserFocus(task.getUserFocus());
        response.setRiskReport(task.getRiskReport());
        response.setGeneratedRequirement(task.getGeneratedRequirement());
        response.setAppliedOperationCount(task.getAppliedOperationCount());
        response.setSkippedOperationCount(task.getSkippedOperationCount());
        response.setSkippedOperationMessages(task.getSkippedOperationMessages());
        response.setRetryable(task.getRetryable());
        response.setRetryCount(task.getRetryCount());
        response.setMaxRetry(task.getMaxRetry());
        response.setLastErrorCode(task.getLastErrorCode());
        response.setErrorMessage(task.getErrorMessage());
        response.setLlmProvider(task.getLlmProvider());
        response.setStartTime(task.getStartTime());
        response.setFinishTime(task.getFinishTime());
        response.setDurationMs(task.getDurationMs());
        response.setCreateTime(task.getCreateTime());
        response.setUpdateTime(task.getUpdateTime());
        return response;
    }

    private List<String> parseJsonList(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return List.of();
            }
            return OBJECT_MAPPER.readValue(json, STRING_LIST_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }

    private int safeInt(Integer value) {
        return safeInt(value, 0);
    }

    private int safeInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }
}
