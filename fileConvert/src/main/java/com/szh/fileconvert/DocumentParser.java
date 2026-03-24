package com.szh.fileconvert;

import com.szh.fileconvert.base.MarkdownResult;
import com.szh.fileconvert.base.ParseContext;

import java.io.File;

public interface DocumentParser {

    boolean supports(String fileType);

    MarkdownResult parse(File file, ParseContext context) throws Exception;
}
