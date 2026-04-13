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

public class AiPdfParserExample2 {

    private static final String TEST_PDF = "北京市朝阳区住宅租赁合同（个人出租）.pdf";

    public static void main(String[] args) throws Exception {
        LLMService llmService = new ArkLLMService(ArkConfigLoader.getApiKey(), ArkConfigLoader.getModel());
        AiPdfParser parser = new AiPdfParser(llmService);
        File pdfFile = TestResourceFiles.require(TEST_PDF);

        ParseContext context = new ParseContext();
        context.setContractMode(true);

        long startTime = System.currentTimeMillis();
        if (parser.supports("pdf")) {
            MarkdownResult result = parser.parse(pdfFile, context);
            long endTime = System.currentTimeMillis();
            System.out.println("Elapsed: " + (endTime - startTime) + "ms");

            System.out.println("=== Markdown Output ===");
            System.out.println(result.getMarkdown());

            File outputFile = new File("target/contractM2.md");
            FileUtils.writeStringToFile(outputFile, result.getMarkdown(), StandardCharsets.UTF_8);
            System.out.println("Saved to: " + outputFile.getAbsolutePath());
        }
    }
}
