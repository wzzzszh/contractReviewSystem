package com.szh.contractReviewSystem.agent.docx.model;

import lombok.Data;

@Data
public class DocxModifyResponse {

    private String inputPath;

    private String outputPath;

    private String modificationRequirement;

    private String resultMessage;
}
