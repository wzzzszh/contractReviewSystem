package com.szh.contractReviewSystem.model.request;

import com.szh.contractReviewSystem.agent.docx.model.DocxModifyPerspective;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CreateReviewTaskRequest {

    @NotNull(message = "sourceFileId must not be null")
    private Long sourceFileId;

    private String taskType;

    private DocxModifyPerspective perspective;

    private String userFocus;

    private String modificationRequirement;
}
