package com.szh.contractReviewSystem.document.convert;

import com.szh.contractReviewSystem.document.model.MarkdownResult;
import com.szh.contractReviewSystem.document.model.ParseContext;
import com.szh.contractReviewSystem.testsupport.TestResourceFiles;
import com.szh.contractReviewSystem.utils.FileUtils;

public class PdfParseMdTest1 {

    private static final String TEST_PDF = "合同审查法律意见书.pdf";

    public static void main(String[] args) throws Exception {
        DocumentToMarkdownConverter converter = new DocumentToMarkdownConverter();

        ParseContext context = new ParseContext();
        context.setContractMode(true);

        MarkdownResult result = converter.convert(TestResourceFiles.require(TEST_PDF), context);

        System.out.println(result.getMarkdown());
        FileUtils.writeFile("target/contract1.md", result.getMarkdown());
    }
}
