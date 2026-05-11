package com.szh.contractReviewSystem.service.review;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;

@Service
public class ReviewTaskExecutor {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 2000;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String ERROR_CODE_PATCH_APPLY_NOT_ACCEPTABLE = "PATCH_APPLY_NOT_ACCEPTABLE";

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

            reviewTaskMapper.updateProgress(taskId, 12);
            DocxModifyResponse modifyResponse = docxDocumentService.modifyDocument(
                    buildModifyRequest(task, sourceFile),
                    progress -> reviewTaskMapper.updateProgress(taskId, progress)
            );

            reviewTaskMapper.updateProgress(taskId, 90);
            FileStorageEntity resultFile = createResultFileRecord(task, sourceFile, modifyResponse);
            String patchContent = buildPatchContent(modifyResponse);
            reviewTaskMapper.markSuccess(
                    taskId,
                    resultFile.getId(),
                    modifyResponse.getRiskReviewReport(),
                    patchContent,
                    modifyResponse.getAppliedOperationCount(),
                    modifyResponse.getSkippedOperationCount(),
                    toJson(modifyResponse.getSkippedOperationMessages())
            );
        } catch (Exception e) {
            reviewTaskMapper.markFailed(
                    taskId,
                    toErrorMessage(e),
                    isRetryableFailure(e),
                    resolveErrorCode(e),
                    null,
                    null,
                    null
            );
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

    private FileStorageEntity createResultFileRecord(ReviewTaskEntity task,
                                                     FileStorageEntity sourceFile,
                                                     DocxModifyResponse modifyResponse) {
        Path outputPath = Path.of(modifyResponse.getOutputPath()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(outputPath)) {
            throw new IllegalStateException("Review result file was not generated: " + outputPath);
        }
        return fileStorageRecordService.createModifiedFileRecord(
                task.getUserId(),
                buildModifiedDisplayName(sourceFile, outputPath),
                outputPath.toString(),
                task.getSourceFileId()
        );
    }

    private String buildModifiedDisplayName(FileStorageEntity sourceFile, Path outputPath) {
        String sourceFileName = sourceFile == null ? null : sourceFile.getFileName();
        if (sourceFileName == null || sourceFileName.trim().isEmpty()) {
            return outputPath.getFileName().toString();
        }

        String normalized = sourceFileName.trim();
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex > 0) {
            return normalized.substring(0, dotIndex) + "-修改稿" + normalized.substring(dotIndex);
        }
        return normalized + "-修改稿.docx";
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

    private boolean isRetryableFailure(Exception e) {
        return true;
    }

    private String resolveErrorCode(Exception e) {
        String message = e == null ? null : e.getMessage();
        if (message != null && message.contains("Patch generation failed after")) {
            return ERROR_CODE_PATCH_APPLY_NOT_ACCEPTABLE;
        }
        return e == null ? "UNKNOWN" : e.getClass().getSimpleName();
    }

    private String toJson(List<String> value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String buildPatchContent(DocxModifyResponse modifyResponse) {
        StringBuilder builder = new StringBuilder();
        builder.append("## 补丁执行结果\n")
                .append("- 已应用操作: ").append(nullToZero(modifyResponse.getAppliedOperationCount())).append("\n")
                .append("- 跳过操作: ").append(nullToZero(modifyResponse.getSkippedOperationCount())).append("\n");

        if (!isBlank(modifyResponse.getWarningMessage())) {
            builder.append("- 警告: ").append(modifyResponse.getWarningMessage().trim()).append("\n");
        }

        String planPath = modifyResponse.getPatchPlanPath();
        if (isBlank(planPath)) {
            appendFallbackRequirement(builder, modifyResponse);
            return builder.toString();
        }

        try {
            Path patchPlanPath = Path.of(planPath).toAbsolutePath().normalize();
            if (!Files.isRegularFile(patchPlanPath)) {
                appendFallbackRequirement(builder, modifyResponse);
                return builder.toString();
            }

            JsonNode root = OBJECT_MAPPER.readTree(patchPlanPath.toFile());
            JsonNode operations = root.path("operations");
            if (!operations.isArray() || operations.size() == 0) {
                appendFallbackRequirement(builder, modifyResponse);
                return builder.toString();
            }

            builder.append("\n## 补丁内容\n");
            int index = 1;
            for (JsonNode operation : operations) {
                String type = text(operation, "type");
                String anchor = text(operation, "anchor");
                String content = summarizeOperation(type, operation);
                builder.append(index++)
                        .append(". [")
                        .append(mapTypeLabel(type))
                        .append("] 锚点: ")
                        .append(isBlank(anchor) ? "-" : anchor)
                        .append("\n")
                        .append("   - 变更: ")
                        .append(content)
                        .append("\n");
            }
        } catch (Exception ex) {
            appendFallbackRequirement(builder, modifyResponse);
        }

        return builder.toString();
    }

    private String summarizeOperation(String type, JsonNode operation) {
        if ("replace_text_in_paragraph".equals(type)) {
            return "将“" + text(operation, "find") + "”替换为“" + text(operation, "replace") + "”";
        }
        if ("append_sentence_to_paragraph".equals(type)) {
            return "在段落后追加: " + text(operation, "append");
        }
        if ("insert_paragraph_after".equals(type)) {
            return "在锚点后新增段落: " + text(operation, "text");
        }
        return "操作参数: " + operation.toString();
    }

    private String mapTypeLabel(String type) {
        if ("replace_text_in_paragraph".equals(type)) {
            return "替换文本";
        }
        if ("append_sentence_to_paragraph".equals(type)) {
            return "追加语句";
        }
        if ("insert_paragraph_after".equals(type)) {
            return "插入段落";
        }
        return isBlank(type) ? "未知操作" : type;
    }

    private void appendFallbackRequirement(StringBuilder builder, DocxModifyResponse modifyResponse) {
        String fallback = modifyResponse.getGeneratedModificationRequirement();
        if (!isBlank(fallback)) {
            builder.append("\n## 回退内容\n").append(fallback.trim()).append("\n");
        }
    }

    private String text(JsonNode node, String field) {
        if (node == null) {
            return "";
        }
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
