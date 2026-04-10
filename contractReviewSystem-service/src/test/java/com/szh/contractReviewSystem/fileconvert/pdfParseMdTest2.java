package com.szh.contractReviewSystem.fileconvert;

import com.szh.contractReviewSystem.utils.FileUtils;
import com.szh.contractReviewSystem.fileconvert.base.DocumentToMarkdownConverter;
import com.szh.contractReviewSystem.fileconvert.base.MarkdownResult;
import com.szh.contractReviewSystem.fileconvert.base.ParseContext;

import java.io.File;

public class pdfParseMdTest2 {
    public static void main(String[] args) throws Exception {
        DocumentToMarkdownConverter converter = new DocumentToMarkdownConverter();

        ParseContext context = new ParseContext();
        context.setContractMode(true);

        MarkdownResult result = converter.convert(new File("src/test/resources/北京市朝阳区住宅租赁合同（个人出租）.pdf"), context);

        System.out.println(result.getMarkdown());

        FileUtils.writeFile("target/contract2.md", result.getMarkdown());
    }
}
