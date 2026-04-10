package com.szh.contractReviewSystem.fileconvert;

import com.szh.contractReviewSystem.fileconvert.pdf.ArkConfigLoader;
import com.szh.contractReviewSystem.fileconvert.pdf.ArkLLMService;
import com.szh.contractReviewSystem.fileconvert.risk.ContractRiskReviewer;

import java.io.File;

public class ContractRiskReviewTest {

    public static void main(String[] args) throws Exception {
        ContractRiskReviewer reviewer = new ContractRiskReviewer(
                new ArkLLMService(ArkConfigLoader.getApiKey(), ArkConfigLoader.getModel())
        );

        try {
            System.out.println("========================================");
            System.out.println("  合同风险审查测试");
            System.out.println("========================================");

            File pdfFile = new File("D:\\JavaExercise\\SLYT\\contractReviewSystem\\contractReviewSystem-service\\src\\test\\resources\\北京市朝阳区住宅租赁合同（个人出租）.pdf");
            System.out.println("\n[审查文件] " + pdfFile.getName());

            ContractRiskReviewer.ReviewResult result = reviewer.review(pdfFile);

            System.out.println("[耗时统计]");
            System.out.println("  PDF转Markdown: " + result.getParseTimeMs() + "ms");
            System.out.println("  LLM风险审查:   " + result.getReviewTimeMs() + "ms");
            System.out.println("  总耗时:         " + result.getTotalTimeMs() + "ms");

            System.out.println("\n========================================");
            System.out.println("  合同风险审查报告");
            System.out.println("========================================");
            System.out.println(result.getReport());

            result.saveReport(new File("target/contract_risk_review_report.md"));
            result.saveMarkdown(new File("target/contract_risk_review_input.md"));

            System.out.println("\n报告已保存到: target/contract_risk_review_report.md");
            System.out.println("中间MD保存到: target/contract_risk_review_input.md");
        } finally {
            reviewer.shutdown();
        }
    }
}
