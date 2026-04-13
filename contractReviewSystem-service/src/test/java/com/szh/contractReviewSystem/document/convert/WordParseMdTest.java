package com.szh.contractReviewSystem.document.convert;

import com.szh.contractReviewSystem.document.model.MarkdownResult;
import com.szh.contractReviewSystem.document.model.ParseContext;
import com.szh.contractReviewSystem.testsupport.TestResourceFiles;
import com.szh.contractReviewSystem.utils.FileUtils;

public class WordParseMdTest {

    private static final String TEST_DOCX = "劳动合同（word范本）.docx";

    public static void main(String[] args) throws Exception {
        DocumentToMarkdownConverter converter = new DocumentToMarkdownConverter();

        ParseContext context = new ParseContext();
        context.setContractMode(true);

        MarkdownResult result = converter.convert(TestResourceFiles.require(TEST_DOCX), context);

        System.out.println(result.getMarkdown());
        FileUtils.writeFile("target/contract2.md", result.getMarkdown());
    }
}
