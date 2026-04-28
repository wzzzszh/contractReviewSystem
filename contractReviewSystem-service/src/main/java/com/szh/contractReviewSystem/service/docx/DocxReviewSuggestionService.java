package com.szh.contractReviewSystem.service.docx;

import com.szh.contractReviewSystem.agent.docx.model.DocxModifyPerspective;

import java.nio.file.Path;

public interface DocxReviewSuggestionService {

    DocxModifyPerspective getPerspective();

    DocxReviewSuggestionResult generateModificationRequirement(Path inputPath, String userFocus);
}
