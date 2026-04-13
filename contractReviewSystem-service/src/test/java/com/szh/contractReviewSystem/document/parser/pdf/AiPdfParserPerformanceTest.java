package com.szh.contractReviewSystem.document.parser.pdf;

import com.szh.contractReviewSystem.document.model.MarkdownResult;
import com.szh.contractReviewSystem.document.model.ParseContext;
import com.szh.contractReviewSystem.llm.LLMService;
import com.szh.contractReviewSystem.llm.ark.ArkConfigLoader;
import com.szh.contractReviewSystem.llm.ark.ArkLLMService;
import com.szh.contractReviewSystem.testsupport.TestResourceFiles;

import java.io.File;

public class AiPdfParserPerformanceTest {

    private static final String TEST_PDF = "北京市朝阳区住宅租赁合同（个人出租）.pdf";

    public static void main(String[] args) throws Exception {
        LLMService llmService = new ArkLLMService(ArkConfigLoader.getApiKey(), ArkConfigLoader.getModel());
        AiPdfParser parser = new AiPdfParser(llmService);
        File pdfFile = TestResourceFiles.require(TEST_PDF);

        ParseContext context = new ParseContext();
        context.setContractMode(false);

        int iterations = 100;
        long totalTime = 0;
        int successCount = 0;
        int failCount = 0;

        System.out.println("Performance test iterations: " + iterations);
        System.out.println("Test file: " + pdfFile.getAbsolutePath());
        System.out.println("==========================================");

        for (int i = 1; i <= iterations; i++) {
            long startTime = System.currentTimeMillis();
            try {
                if (parser.supports("pdf")) {
                    MarkdownResult result = parser.parse(pdfFile, context);
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    totalTime += duration;
                    successCount++;

                    if (result == null) {
                        throw new IllegalStateException("Parser returned null result");
                    }
                    System.out.println("Run " + i + ": success, elapsed=" + duration + "ms");
                } else {
                    failCount++;
                    System.out.println("Run " + i + ": unsupported pdf");
                }
            } catch (Exception e) {
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                totalTime += duration;
                failCount++;

                System.out.println("Run " + i + ": failed, elapsed=" + duration + "ms");
                System.out.println("Error: " + e.getMessage());
            }
        }

        System.out.println("==========================================");
        System.out.println("Success count: " + successCount);
        System.out.println("Fail count: " + failCount);
        System.out.println("Total elapsed: " + totalTime + "ms");
        if (successCount > 0) {
            double averageTime = (double) totalTime / successCount;
            System.out.println("Average elapsed: " + String.format("%.2f", averageTime) + "ms");
        }
    }
}
