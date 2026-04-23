package com.szh.contractReviewSystem.controller;

import com.szh.contractReviewSystem.agent.docx.DocxSkillAgentService;
import com.szh.contractReviewSystem.agent.docx.model.DocxModifyRequest;
import com.szh.contractReviewSystem.agent.docx.model.DocxModifyResponse;
import com.szh.contractReviewSystem.common.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.nio.file.Path;
import java.util.Locale;

@RestController
@RequestMapping("/api/docx-agent")
public class DocxSkillAgentController extends BaseController {

    private final DocxSkillAgentService docxSkillAgentService;

    public DocxSkillAgentController(DocxSkillAgentService docxSkillAgentService) {
        this.docxSkillAgentService = docxSkillAgentService;
    }

    @PostMapping("/modify")
    public Result<DocxModifyResponse> modifyDocument(@Valid @RequestBody DocxModifyRequest request) {
        try {
            Path inputPath = resolveDocxPath(request.getInputPath(), "inputPath");
            Path outputPath = resolveOutputPath(request, inputPath);

            String resultMessage = docxSkillAgentService.modifyDocument(
                    inputPath,
                    outputPath,
                    request.getModificationRequirement().trim()
            );

            DocxModifyResponse response = new DocxModifyResponse();
            response.setInputPath(inputPath.toString());
            response.setOutputPath(outputPath.toString());
            response.setModificationRequirement(request.getModificationRequirement().trim());
            response.setResultMessage(resultMessage);
            return success("DOCX文档修改成功", response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return error(e.getMessage());
        }
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
            throw new IllegalArgumentException(fieldName + "不能为空");
        }

        Path path = Path.of(normalizedPath).toAbsolutePath().normalize();
        if (!path.toString().toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw new IllegalArgumentException(fieldName + "必须是.docx文件路径");
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
}
