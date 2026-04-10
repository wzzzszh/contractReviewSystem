package com.szh.contractReviewSystem.fileconvert;

import com.szh.contractReviewSystem.fileconvert.base.MarkdownResult;
import com.szh.contractReviewSystem.fileconvert.base.ParseContext;

import java.io.File;

public interface DocumentParser {

    boolean supports(String fileType);

    MarkdownResult parse(File file, ParseContext context) throws Exception;
}
