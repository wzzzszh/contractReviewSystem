package com.szh.contractReviewSystem.agent.docx.model;

import lombok.Data;

import java.util.List;

@Data
public class DocxModifyResponse {

    private String inputPath;

    private String outputPath;

    private DocxModifyPerspective perspective;

    private String modificationRequirement;

    private String generatedModificationRequirement;

    private String riskReviewReport;

    private String patchPlanPath;

    private Integer appliedOperationCount;

    private Integer skippedOperationCount;

    private List<String> skippedOperationMessages;

    private String warningMessage;

    private String resultMessage;
}
