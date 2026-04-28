package com.szh.contractReviewSystem.service.review;

import com.szh.contractReviewSystem.agent.docx.model.DocxModifyPerspective;
import com.szh.contractReviewSystem.agent.docx.model.DocxModifyRequest;
import com.szh.contractReviewSystem.agent.docx.model.DocxModifyResponse;
import com.szh.contractReviewSystem.entity.file.FileStorageEntity;
import com.szh.contractReviewSystem.entity.review.ReviewTaskEntity;
import com.szh.contractReviewSystem.mapper.review.ReviewTaskMapper;
import com.szh.contractReviewSystem.service.db.FileStorageRecordService;
import com.szh.contractReviewSystem.service.docx.DocxDocumentService;
import com.szh.contractReviewSystem.utils.UserContextHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ReviewTaskExecutor {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 2000;

    private final ReviewTaskMapper reviewTaskMapper;
    private final FileStorageRecordService fileStorageRecordService;
    private final DocxDocumentService docxDocumentService;

    public ReviewTaskExecutor(ReviewTaskMapper reviewTaskMapper,
                              FileStorageRecordService fileStorageRecordService,
                              DocxDocumentService docxDocumentService) {
        this.reviewTaskMapper = reviewTaskMapper;
        this.fileStorageRecordService = fileStorageRecordService;
        this.docxDocumentService = docxDocumentService;
    }

    @Async("reviewAsyncExecutor")
    public void executeReviewTask(Long taskId) {
        ReviewTaskEntity task = reviewTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }

        UserContextHolder.setUserId(task.getUserId());
        try {
            reviewTaskMapper.markRunning(taskId, 10);
            FileStorageEntity sourceFile =
                    fileStorageRecordService.getOwnedActiveRecord(task.getSourceFileId(), task.getUserId());

            reviewTaskMapper.updateProgress(taskId, 30);
            DocxModifyResponse modifyResponse = docxDocumentService.modifyDocument(buildModifyRequest(task, sourceFile));

            reviewTaskMapper.updateProgress(taskId, 90);
            FileStorageEntity resultFile = createResultFileRecord(task, modifyResponse);
            reviewTaskMapper.markSuccess(
                    taskId,
                    resultFile.getId(),
                    modifyResponse.getRiskReviewReport(),
                    modifyResponse.getGeneratedModificationRequirement()
            );
        } catch (Exception e) {
            reviewTaskMapper.markFailed(taskId, toErrorMessage(e));
        } finally {
            UserContextHolder.clear();
        }
    }

    private DocxModifyRequest buildModifyRequest(ReviewTaskEntity task, FileStorageEntity sourceFile) {
        DocxModifyRequest request = new DocxModifyRequest();
        request.setInputPath(sourceFile.getFilePath());
        request.setPerspective(DocxModifyPerspective.fromValue(task.getPerspective()));
        request.setModificationRequirement(task.getUserFocus());
        return request;
    }

    private FileStorageEntity createResultFileRecord(ReviewTaskEntity task, DocxModifyResponse modifyResponse) {
        Path outputPath = Path.of(modifyResponse.getOutputPath()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(outputPath)) {
            throw new IllegalStateException("Review result file was not generated: " + outputPath);
        }
        return fileStorageRecordService.createModifiedFileRecord(
                task.getUserId(),
                outputPath.getFileName().toString(),
                outputPath.toString(),
                task.getSourceFileId()
        );
    }

    private String toErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = e.getClass().getSimpleName();
        }
        if (message.length() > MAX_ERROR_MESSAGE_LENGTH) {
            return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
        }
        return message;
    }
}
