package com.szh.contractReviewSystem.document.convert;

import com.szh.contractReviewSystem.document.model.MarkdownResult;
import com.szh.contractReviewSystem.document.model.ParseContext;
import com.szh.contractReviewSystem.testsupport.TestResourceFiles;
import com.szh.contractReviewSystem.utils.FileUtils;

public class PdfParseMdTest2 {

    private static final String TEST_PDF = "北京市朝阳区住宅租赁合同（个人出租）.pdf";

    public static void main(String[] args) throws Exception {
        DocumentToMarkdownConverter converter = new DocumentToMarkdownConverter();

        ParseContext context = new ParseContext();
        context.setContractMode(true);

        MarkdownResult result = converter.convert(TestResourceFiles.require(TEST_PDF), context);

        System.out.println(result.getMarkdown());
        FileUtils.writeFile("target/contract2.md", result.getMarkdown());
    }
}
