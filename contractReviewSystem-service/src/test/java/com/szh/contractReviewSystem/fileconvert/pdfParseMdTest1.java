package com.szh.contractReviewSystem.fileconvert;

import com.szh.contractReviewSystem.utils.FileUtils;
import com.szh.contractReviewSystem.fileconvert.base.DocumentToMarkdownConverter;
import com.szh.contractReviewSystem.fileconvert.base.MarkdownResult;
import com.szh.contractReviewSystem.fileconvert.base.ParseContext;

import java.io.File;

public class pdfParseMdTest1 {
    public static void main(String[] args) throws Exception {
        DocumentToMarkdownConverter converter = new DocumentToMarkdownConverter();

        ParseContext context = new ParseContext();
        context.setContractMode(true);

        MarkdownResult result = converter.convert(new File("src/test/resources/合同审查法律意见书.pdf"), context);

        System.out.println(result.getMarkdown());

        FileUtils.writeFile("target/contract1.md", result.getMarkdown());
    }
}
