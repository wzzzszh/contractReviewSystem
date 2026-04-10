package com.szh.contractReviewSystem.fileconvert;

import com.szh.contractReviewSystem.utils.FileUtils;
import com.szh.contractReviewSystem.fileconvert.base.DocumentToMarkdownConverter;
import com.szh.contractReviewSystem.fileconvert.base.MarkdownResult;
import com.szh.contractReviewSystem.fileconvert.base.ParseContext;

import java.io.File;

public class wordParseMdTest {
    public static void main(String[] args) throws Exception {
        DocumentToMarkdownConverter converter = new DocumentToMarkdownConverter();

        ParseContext context = new ParseContext();
        context.setContractMode(true);

        MarkdownResult result = converter.convert(new File("src/test/resources/劳动合同（word范本）.docx"), context);

        System.out.println(result.getMarkdown());

        FileUtils.writeFile("target/contract2.md", result.getMarkdown());
    }
}
