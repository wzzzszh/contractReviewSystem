package com.szh.contractReviewSystem.service.review;

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
        reviewTaskMapper.insert(task);

        try {
            reviewTaskExecutor.executeReviewTask(task.getId());
        } catch (RuntimeException e) {
            reviewTaskMapper.markFailed(task.getId(), "review task queue rejected submission");
            throw new CustomException(BusinessExceptionEnum.REVIEW_FAILED,
                    "review task queue rejected submission", e);
        }
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
        response.setErrorMessage(task.getErrorMessage());
        response.setCreateTime(task.getCreateTime());
        response.setUpdateTime(task.getUpdateTime());
        return response;
    }
}
