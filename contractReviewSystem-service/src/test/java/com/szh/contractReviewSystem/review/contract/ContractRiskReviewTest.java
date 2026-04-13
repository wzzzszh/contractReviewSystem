package com.szh.contractReviewSystem.review.contract;

import com.szh.contractReviewSystem.llm.ark.ArkConfigLoader;
import com.szh.contractReviewSystem.llm.ark.ArkLLMService;
import com.szh.contractReviewSystem.testsupport.TestResourceFiles;

import java.io.File;

public class ContractRiskReviewTest {

    private static final String TEST_PDF = "北京市朝阳区住宅租赁合同（个人出租）.pdf";

    public static void main(String[] args) throws Exception {
        ContractRiskReviewer reviewer = new ContractRiskReviewer(
                new ArkLLMService(ArkConfigLoader.getApiKey(), ArkConfigLoader.getModel())
        );

        try {
            File pdfFile = TestResourceFiles.require(TEST_PDF);
            System.out.println("Review file: " + pdfFile.getName());

            ContractRiskReviewer.ReviewOptions options = ContractRiskReviewer.ReviewOptions.of(
                    ContractRiskReviewer.ReviewPerspective.BOTH_SIDES
            );
            System.out.println("Perspective: " + options.getPerspective().getDisplayName());

            ContractRiskReviewer.ReviewResult result = reviewer.review(pdfFile, true, options);
            System.out.println("Parse elapsed: " + result.getParseTimeMs() + "ms");
            System.out.println("Review elapsed: " + result.getReviewTimeMs() + "ms");
            System.out.println("Total elapsed: " + result.getTotalTimeMs() + "ms");
            System.out.println(result.getReport());

            result.saveReport(new File("target/contract_risk_review_report.md"));
            result.saveMarkdown(new File("target/contract_risk_review_input.md"));
        } finally {
            reviewer.shutdown();
        }
    }
}
