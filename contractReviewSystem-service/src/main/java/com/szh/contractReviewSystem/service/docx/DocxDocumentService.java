package com.szh.contractReviewSystem.service.docx;

import com.szh.contractReviewSystem.agent.docx.DocxSkillAgentService;
import com.szh.contractReviewSystem.agent.docx.model.DocxModifyPerspective;
import com.szh.contractReviewSystem.agent.docx.model.DocxModifyRequest;
import com.szh.contractReviewSystem.agent.docx.model.DocxModifyResponse;
import com.szh.contractReviewSystem.config.FileLifecycleProperties;
import com.szh.contractReviewSystem.exception.BusinessExceptionEnum;
import com.szh.contractReviewSystem.exception.CustomException;
import com.szh.contractReviewSystem.service.db.FileStorageRecordService;
import com.szh.contractReviewSystem.utils.UserContextHolder;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DocxDocumentService {

    private final DocxSkillAgentService docxSkillAgentService;
    private final Map<DocxModifyPerspective, DocxReviewSuggestionService> suggestionServices;
    private final FileStorageRecordService fileStorageRecordService;
    private final FileLifecycleProperties fileLifecycleProperties;

    public DocxDocumentService(DocxSkillAgentService docxSkillAgentService,
                               List<DocxReviewSuggestionService> suggestionServices,
                               FileStorageRecordService fileStorageRecordService,
                               FileLifecycleProperties fileLifecycleProperties) {
        this.docxSkillAgentService = docxSkillAgentService;
        this.fileStorageRecordService = fileStorageRecordService;
        this.fileLifecycleProperties = fileLifecycleProperties;
        this.suggestionServices = new EnumMap<>(DocxModifyPerspective.class);
        for (DocxReviewSuggestionService suggestionService : suggestionServices) {
            DocxReviewSuggestionService existing =
                    this.suggestionServices.put(suggestionService.getPerspective(), suggestionService);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate docx review suggestion service for perspective: "
                                + suggestionService.getPerspective()
                );
            }
        }
    }

    public DocxModifyResponse modifyDocument(DocxModifyRequest request) {
        // 1. 解析并校验输入/输出路径，确保只处理 DOCX 文件。
        Path inputPath = resolveDocxPath(request.getInputPath(), "inputPath");
        Path outputPath = resolveOutputPath(request, inputPath);

        // 2. 用户可选填写额外关注点；未填写时由风险审查流程自行判断重点。
        String userFocus = normalizeOptionalText(request.getModificationRequirement());

        // 3. 根据请求选择甲方或乙方视角，默认按甲方视角生成修改方案。
        DocxModifyPerspective perspective = DocxModifyPerspective.resolveOrDefault(request.getPerspective());

        // 4. 先生成“风险提示报告”，再基于风险提示报告生成可执行的修改要求。
        DocxReviewSuggestionResult suggestionResult = getSuggestionService(perspective)
                .generateModificationRequirement(inputPath, userFocus);
        String generatedRequirement = suggestionResult.modificationRequirement();

        // 5. 将修改要求交给 DOCX 补丁管线，真正修改 Word 文档。
        DocxSkillAgentService.ModifyDocumentResult modifyResult =
                docxSkillAgentService.modifyDocument(inputPath, outputPath, generatedRequirement);
        registerAgentWorkDirectory(modifyResult.workDirectory());

        // 6. 返回修改结果，同时把风险提示和最终修改要求回传，方便前端展示和排查。
        DocxModifyResponse response = new DocxModifyResponse();
        response.setInputPath(inputPath.toString());
        response.setOutputPath(modifyResult.outputDocument().toString());
        response.setPerspective(perspective);
        response.setModificationRequirement(userFocus);
        response.setGeneratedModificationRequirement(generatedRequirement);
        response.setRiskReviewReport(suggestionResult.riskReviewReport());
        response.setPatchPlanPath(modifyResult.patchPlanFile().toString());
        response.setAppliedOperationCount(modifyResult.appliedOperationCount());
        response.setSkippedOperationCount(modifyResult.skippedOperationCount());
        response.setSkippedOperationMessages(modifyResult.skippedOperationMessages());
        response.setWarningMessage(modifyResult.warningMessage());
        response.setResultMessage(modifyResult.message());
        return response;
    }

    private void registerAgentWorkDirectory(Path workDirectory) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null || workDirectory == null) {
            return;
        }
        LocalDateTime expireTime = LocalDateTime.now().plusHours(fileLifecycleProperties.getTempTtlHours());
        fileStorageRecordService.createAgentWorkRecord(userId, workDirectory, expireTime);
    }

    private DocxReviewSuggestionService getSuggestionService(DocxModifyPerspective perspective) {
        DocxReviewSuggestionService suggestionService = suggestionServices.get(perspective);
        if (suggestionService == null) {
            throw new CustomException(BusinessExceptionEnum.DOCX_MODIFY_FAILED,
                    "Missing docx review suggestion service for perspective: " + perspective);
        }
        return suggestionService;
    }

    private Path resolveOutputPath(DocxModifyRequest request, Path inputPath) {
        if (isBlank(request.getOutputPath())) {
            return buildDefaultOutputPath(inputPath);
        }
        return resolveDocxPath(request.getOutputPath(), "outputPath");
    }

    private Path resolveDocxPath(String rawPath, String fieldName) {
        String normalizedPath = rawPath == null ? "" : rawPath.trim();
        if (normalizedPath.isEmpty()) {
            throw new CustomException(BusinessExceptionEnum.REVIEW_PARAMETER_ERROR, fieldName + "不能为空");
        }

        Path path = Path.of(normalizedPath).toAbsolutePath().normalize();
        if (!path.toString().toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw new CustomException(BusinessExceptionEnum.REVIEW_PARAMETER_ERROR,
                    fieldName + "必须是.docx文件路径");
        }
        return path;
    }

    private Path buildDefaultOutputPath(Path inputPath) {
        String fileName = inputPath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        String extension = dotIndex >= 0 ? fileName.substring(dotIndex) : ".docx";
        String outputFileName = baseName + "-modified" + extension;
        Path parent = inputPath.getParent();
        if (parent != null) {
            return parent.resolve(outputFileName).toAbsolutePath().normalize();
        }
        return Path.of(outputFileName).toAbsolutePath().normalize();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeOptionalText(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
