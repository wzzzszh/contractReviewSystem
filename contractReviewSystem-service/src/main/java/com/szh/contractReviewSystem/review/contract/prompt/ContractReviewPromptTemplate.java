package com.szh.contractReviewSystem.review.contract.prompt;

import com.szh.contractReviewSystem.review.contract.ContractRiskReviewer;

import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于 LangChain4j 的 Markdown Prompt 模板加载器。
 * <p>
 * 将合同审查流程外置为 Markdown 文件，便于后续只调 Prompt 而不改 Java 代码。
 */
public class ContractReviewPromptTemplate {

    private static final String TEMPLATE_PATH = "review/contract/prompts/contract-risk-review-prompt.md";
    private static final String TABLE_HEADER =
            "| 序号 | 风险等级 | 问题类型 | 合同条款位置 | 问题描述 | 利害关系 | 调整方向 |\n" +
            "|------|----------|----------|--------------|----------|----------|----------|";

    private final PromptTemplate promptTemplate;

    public ContractReviewPromptTemplate() {
        try (InputStream is = ContractReviewPromptTemplate.class.getClassLoader().getResourceAsStream(TEMPLATE_PATH)) {
            if (is == null) {
                throw new IllegalStateException("Prompt template not found in classpath: " + TEMPLATE_PATH);
            }
            String template = IOUtils.toString(is, StandardCharsets.UTF_8);
            this.promptTemplate = PromptTemplate.from(template);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load contract review prompt template", e);
        }
    }

    public String render(String contractMarkdown, String riskKnowledge, ContractRiskReviewer.ReviewOptions options) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("perspectiveDisplayName", options.getPerspective().getDisplayName());
        variables.put("perspectiveInstruction", options.getPerspective().getInstructionLabel());
        variables.put("outputSections", buildOutputSections(options.getPerspective()));
        variables.put("riskKnowledge", riskKnowledge == null ? "" : riskKnowledge);
        variables.put("contractMarkdown", contractMarkdown == null ? "" : contractMarkdown);

        Prompt prompt = promptTemplate.apply(variables);
        return prompt.text();
    }

    private String buildOutputSections(ContractRiskReviewer.ReviewPerspective perspective) {
        if (perspective == ContractRiskReviewer.ReviewPerspective.BOTH_SIDES) {
            return new StringBuilder()
                    .append("### 对甲方不利\n")
                    .append(TABLE_HEADER)
                    .append("\n\n")
                    .append("### 对乙方不利\n")
                    .append(TABLE_HEADER)
                    .append("\n\n")
                    .append("### 双方共同风险\n")
                    .append(TABLE_HEADER)
                    .toString();
        }

        return new StringBuilder()
                .append("### 对当前立场不利\n")
                .append(TABLE_HEADER)
                .append("\n\n")
                .append("### 对相对方明显不利\n")
                .append(TABLE_HEADER)
                .append("\n\n")
                .append("### 双方共同风险\n")
                .append(TABLE_HEADER)
                .toString();
    }
}
