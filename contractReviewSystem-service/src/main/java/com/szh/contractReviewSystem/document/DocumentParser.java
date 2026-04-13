package com.szh.contractReviewSystem.document;

import com.szh.contractReviewSystem.document.model.MarkdownResult;
import com.szh.contractReviewSystem.document.model.ParseContext;

import java.io.File;

public interface DocumentParser {

    boolean supports(String fileType);

    MarkdownResult parse(File file, ParseContext context) throws Exception;
}
