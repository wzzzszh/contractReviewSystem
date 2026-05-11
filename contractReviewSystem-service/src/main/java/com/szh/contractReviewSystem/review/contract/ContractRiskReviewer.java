package com.szh.contractReviewSystem.review.contract;

import com.szh.contractReviewSystem.document.model.MarkdownResult;
import com.szh.contractReviewSystem.document.model.ParseContext;
import com.szh.contractReviewSystem.document.parser.pdf.AiPdfParser;
import com.szh.contractReviewSystem.llm.LLMService;
import com.szh.contractReviewSystem.review.contract.prompt.ContractReviewPromptTemplate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 合同风险审查器。
 * <p>
 * 负责将 PDF 合同解析为 Markdown 文本，并结合风险知识库构造提示词，
 * 调用大模型生成结构化的合同风险审查报告。
 */
public class ContractRiskReviewer {

    private final LLMService llmService;
    private final AiPdfParser pdfParser;
    private final ContractReviewPromptTemplate promptTemplate;
    /**
     * 风险知识库内容，用于指导大模型进行合同审查。
     */
    private String riskKnowledge;

    /**
     * 使用默认风险知识库创建审查器。
     *
     * @param llmService 大模型调用服务
     */
    public ContractRiskReviewer(LLMService llmService) {
        this.llmService = llmService;
        this.pdfParser = new AiPdfParser(llmService);
        this.promptTemplate = new ContractReviewPromptTemplate();
        try {
            this.riskKnowledge = loadDefaultRiskKnowledge();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load default risk knowledge", e);
        }
    }

    /**
     * 使用自定义风险知识库创建审查器。
     *
     * @param llmService 大模型调用服务
     * @param customKnowledgePath 自定义风险知识库文件路径
     */
    public ContractRiskReviewer(LLMService llmService, String customKnowledgePath) {
        this.llmService = llmService;
        this.pdfParser = new AiPdfParser(llmService);
        this.promptTemplate = new ContractReviewPromptTemplate();
        try {
            this.riskKnowledge = loadRiskKnowledge(customKnowledgePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load risk knowledge from: " + customKnowledgePath, e);
        }
    }

    /**
     * 使用默认合同模式审查 PDF 文件。
     *
     * @param pdfFile 待审查的 PDF 合同文件
     * @return 审查结果，包含风险报告、解析后的 Markdown 以及耗时信息
     * @throws Exception 审查过程中发生异常时抛出
     */
    public ReviewResult review(File pdfFile) throws Exception {
        return review(pdfFile, true, ReviewOptions.defaultOptions());
    }

    /**
     * 审查 PDF 文件，并根据模式决定解析策略。
     *
     * @param pdfFile 待审查的 PDF 文件
     * @param contractMode 是否使用合同模式解析
     * @return 审查结果，包含风险报告、解析后的 Markdown 以及各阶段耗时
     * @throws Exception PDF 解析或大模型调用失败时抛出
     */
    public ReviewResult review(File pdfFile, boolean contractMode) throws Exception {
        return review(pdfFile, contractMode, ReviewOptions.defaultOptions());
    }

    /**
     * 审查 PDF 文件，并根据模式和审查选项决定分析策略。
     *
     * @param pdfFile 待审查的 PDF 文件
     * @param contractMode 是否使用合同模式解析
     * @param options 审查选项，控制审查立场与输出限制
     * @return 审查结果，包含风险报告、解析后的 Markdown 以及各阶段耗时
     * @throws Exception PDF 解析或大模型调用失败时抛出
     */
    public ReviewResult review(File pdfFile, boolean contractMode, ReviewOptions options) throws Exception {
        long startTime = System.currentTimeMillis();

        ParseContext context = new ParseContext();
        context.setContractMode(contractMode);
        MarkdownResult mdResult = pdfParser.parse(pdfFile, context);

        long parseTime = System.currentTimeMillis() - startTime;

        String prompt = buildReviewPrompt(mdResult.getMarkdown(), options);

        long reviewStart = System.currentTimeMillis();
        String report = llmService.call(prompt);
        long reviewTime = System.currentTimeMillis() - reviewStart;

        long totalTime = System.currentTimeMillis() - startTime;
        return new ReviewResult(report, mdResult.getMarkdown(), parseTime, reviewTime, totalTime);
    }

    /**
     * 直接基于已有 Markdown 文本进行风险审查。
     *
     * @param markdownText 合同的 Markdown 文本内容
     * @return 审查结果，解析耗时为 0，仅统计审查阶段和总耗时
     * @throws Exception 大模型调用失败时抛出
     */
    public ReviewResult reviewMarkdown(String markdownText) throws Exception {
        return reviewMarkdown(markdownText, ReviewOptions.defaultOptions());
    }

    /**
     * 直接基于已有 Markdown 文本进行风险审查，并支持自定义审查选项。
     *
     * @param markdownText 合同的 Markdown 文本内容
     * @param options 审查选项，控制审查立场与输出限制
     * @return 审查结果，解析耗时为 0，仅统计审查阶段和总耗时
     * @throws Exception 大模型调用失败时抛出
     */
    public ReviewResult reviewMarkdown(String markdownText, ReviewOptions options) throws Exception {
        long startTime = System.currentTimeMillis();
        String prompt = buildReviewPrompt(markdownText, options);
        String report = llmService.call(prompt);
        long totalTime = System.currentTimeMillis() - startTime;
        return new ReviewResult(report, markdownText, 0, totalTime, totalTime);
    }

    /**
     * 动态设置风险知识库内容。
     *
     * @param riskKnowledge 风险知识库文本
     */
    public void setRiskKnowledge(String riskKnowledge) {
        this.riskKnowledge = riskKnowledge;
    }

    /**
     * 关闭底层大模型服务中可能存在的资源。
     */
    public void shutdown() {
        llmService.shutdown();
    }

    /**
     * 构造合同风险审查提示词。
     *
     * @param contractMarkdown 解析后的合同 Markdown 文本
     * @return 发送给大模型的完整提示词
     */
    private String buildReviewPrompt(String contractMarkdown, ReviewOptions options) {
        ReviewOptions reviewOptions = options == null ? ReviewOptions.defaultOptions() : options;
        return promptTemplate.render(contractMarkdown, riskKnowledge, reviewOptions);
    }

    /**
     * 审查立场，控制模型站在哪一方分析合同利害关系。
     */
    public enum ReviewPerspective {
        PARTY_A("甲方视角", "优先识别对甲方不利、限制甲方权利或加重甲方责任的条款，同时指出对乙方过度不利可能导致失衡或争议的条款"),
        PARTY_B("乙方视角", "优先识别对乙方不利、限制乙方权利或加重乙方责任的条款，同时指出对甲方过度不利可能导致失衡或争议的条款"),
        BOTH_SIDES("双方对照视角", "分别从甲方和乙方立场对照分析，并额外识别双方共同风险");

        private final String displayName;
        private final String instructionLabel;

        ReviewPerspective(String displayName, String instructionLabel) {
            this.displayName = displayName;
            this.instructionLabel = instructionLabel;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getInstructionLabel() {
            return instructionLabel;
        }
    }

    /**
     * 审查选项，用于控制立场化审查流程。
     */
    public static class ReviewOptions {

        private final ReviewPerspective perspective;

        private ReviewOptions(ReviewPerspective perspective) {
            this.perspective = perspective == null ? ReviewPerspective.BOTH_SIDES : perspective;
        }

        public static ReviewOptions of(ReviewPerspective perspective) {
            return new ReviewOptions(perspective);
        }

        public static ReviewOptions defaultOptions() {
            return new ReviewOptions(ReviewPerspective.BOTH_SIDES);
        }

        public ReviewPerspective getPerspective() {
            return perspective;
        }
    }

    /**
     * 从类路径中加载默认风险知识库。
     *
     * @return 默认风险知识库文本
     * @throws Exception 读取资源文件失败时抛出
     */
    private static String loadDefaultRiskKnowledge() throws Exception {
        InputStream is = ContractRiskReviewer.class.getClassLoader()
                .getResourceAsStream("review/contract/knowledge/risk_knowledge.txt");
        if (is == null) {
            throw new RuntimeException("Default risk knowledge file 'review/contract/knowledge/risk_knowledge.txt' not found in classpath");
        }
        return IOUtils.toString(is, StandardCharsets.UTF_8);
    }

    /**
     * 从指定文件路径加载风险知识库。
     *
     * @param path 风险知识库文件路径
     * @return 风险知识库文本
     * @throws Exception 文件不存在或读取失败时抛出
     */
    private static String loadRiskKnowledge(String path) throws Exception {
        File file = new File(path);
        if (!file.exists()) {
            throw new RuntimeException("Risk knowledge file not found: " + path);
        }
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    /**
     * 合同风险审查结果。
     * <p>
     * 封装审查报告、解析得到的 Markdown 内容以及各阶段耗时信息。
     */
    public static class ReviewResult {

        private final String report;
        private final String markdown;
        private final long parseTimeMs;
        private final long reviewTimeMs;
        private final long totalTimeMs;

        /**
         * 创建审查结果对象。
         *
         * @param report 风险审查报告内容
         * @param markdown 解析后的合同 Markdown 内容
         * @param parseTimeMs PDF 解析耗时（毫秒）
         * @param reviewTimeMs 风险审查耗时（毫秒）
         * @param totalTimeMs 总耗时（毫秒）
         */
        public ReviewResult(String report, String markdown,
                           long parseTimeMs, long reviewTimeMs, long totalTimeMs) {
            this.report = report;
            this.markdown = markdown;
            this.parseTimeMs = parseTimeMs;
            this.reviewTimeMs = reviewTimeMs;
            this.totalTimeMs = totalTimeMs;
        }

        /** @return 风险审查报告文本 */
        public String getReport() { return report; }
        /** @return 合同 Markdown 文本 */
        public String getMarkdown() { return markdown; }
        /** @return PDF 解析耗时（毫秒） */
        public long getParseTimeMs() { return parseTimeMs; }
        /** @return 风险审查耗时（毫秒） */
        public long getReviewTimeMs() { return reviewTimeMs; }
        /** @return 总耗时（毫秒） */
        public long getTotalTimeMs() { return totalTimeMs; }

        /**
         * 将风险审查报告保存到文件。
         *
         * @param outputFile 输出文件
         * @throws Exception 文件写入失败时抛出
         */
        public void saveReport(File outputFile) throws Exception {
            FileUtils.writeStringToFile(outputFile, report, StandardCharsets.UTF_8);
        }

        /**
         * 将合同 Markdown 内容保存到文件。
         *
         * @param outputFile 输出文件
         * @throws Exception 文件写入失败时抛出
         */
        public void saveMarkdown(File outputFile) throws Exception {
            FileUtils.writeStringToFile(outputFile, markdown, StandardCharsets.UTF_8);
        }

        @Override
        public String toString() {
            return "ReviewResult{" +
                    "reportLength=" + report.length() +
                    ", parseTime=" + parseTimeMs + "ms" +
                    ", reviewTime=" + reviewTimeMs + "ms" +
                    ", totalTime=" + totalTimeMs + "ms" +
                    '}';
        }
    }
}
