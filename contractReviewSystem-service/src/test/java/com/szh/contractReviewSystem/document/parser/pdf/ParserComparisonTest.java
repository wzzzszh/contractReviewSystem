package com.szh.contractReviewSystem.document.parser.pdf;

import com.szh.contractReviewSystem.document.model.MarkdownResult;
import com.szh.contractReviewSystem.document.model.ParseContext;
import com.szh.contractReviewSystem.llm.LLMService;
import com.szh.contractReviewSystem.llm.ark.ArkConfigLoader;
import com.szh.contractReviewSystem.llm.ark.ArkLLMService;
import com.szh.contractReviewSystem.testsupport.TestResourceFiles;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class ParserComparisonTest {

    private static final String TEST_PDF = "北京市朝阳区住宅租赁合同（个人出租）.pdf";

    public static void main(String[] args) throws Exception {
        LLMService llmService = new ArkLLMService(ArkConfigLoader.getApiKey(), ArkConfigLoader.getModel());
        File pdfFile = TestResourceFiles.require(TEST_PDF);

        ParseContext context = new ParseContext();
        context.setContractMode(true);

        System.out.println("========================================");
        System.out.println("PDF Parser Comparison");
        System.out.println("========================================");
        System.out.println("Test file: " + pdfFile.getName());

        AiPdfParser2 parser2 = new AiPdfParser2(llmService);
        long start2 = System.currentTimeMillis();
        MarkdownResult result2 = parser2.parse(pdfFile, context);
        long end2 = System.currentTimeMillis();
        FileUtils.writeStringToFile(new File("target/parser2_result.md"), result2.getMarkdown(), StandardCharsets.UTF_8);

        AiPdfParser parser1 = new AiPdfParser(llmService);
        parser1.setParallelThreads(4);
        long start1 = System.currentTimeMillis();
        MarkdownResult result1 = parser1.parse(pdfFile, context);
        long end1 = System.currentTimeMillis();
        FileUtils.writeStringToFile(new File("target/parser1_result.md"), result1.getMarkdown(), StandardCharsets.UTF_8);

        System.out.println("Parser2 elapsed: " + (end2 - start2) + "ms");
        System.out.println("Parser1 elapsed: " + (end1 - start1) + "ms");
        System.out.println("Saved: target/parser2_result.md");
        System.out.println("Saved: target/parser1_result.md");
    }
}
