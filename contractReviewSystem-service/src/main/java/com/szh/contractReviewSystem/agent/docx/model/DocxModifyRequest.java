package com.szh.contractReviewSystem.agent.docx.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class DocxModifyRequest {

    @NotBlank(message = "inputPath不能为空")
    private String inputPath;

    private String outputPath;

    private String modificationRequirement;

    private DocxModifyPerspective perspective;
}
