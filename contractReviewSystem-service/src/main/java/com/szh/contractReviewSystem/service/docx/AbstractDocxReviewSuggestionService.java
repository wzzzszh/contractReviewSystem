package com.szh.contractReviewSystem.service.docx;

import com.szh.contractReviewSystem.agent.docx.DocxSkillAgentProperties;
import com.szh.contractReviewSystem.llm.LLMService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDocxReviewSuggestionService implements DocxReviewSuggestionService {

    private static final int MAX_CONTRACT_TEXT_LENGTH = 16000;

    private final LLMService llmService;
    private final LegalContractRiskReviewService legalContractRiskReviewService;

    protected AbstractDocxReviewSuggestionService(DocxSkillAgentProperties properties,
                                                  LLMService llmService,
                                                  LegalContractRiskReviewService legalContractRiskReviewService) {
        this.llmService = llmService;
        this.legalContractRiskReviewService = legalContractRiskReviewService;
    }

    @Override
    public DocxReviewSuggestionResult generateModificationRequirement(Path inputPath, String userFocus) {
        return generateModificationRequirement(inputPath, userFocus, ReviewProgressReporter.NOOP);
    }

    @Override
    public DocxReviewSuggestionResult generateModificationRequirement(Path inputPath,
                                                                      String userFocus,
                                                                      ReviewProgressReporter progressReporter) {
        ReviewProgressReporter reporter = progressReporter == null ? ReviewProgressReporter.NOOP : progressReporter;
        reporter.updateProgress(20);
        String contractText = extractContractText(inputPath);

        reporter.updateProgress(35);
        String riskReviewReport = legalContractRiskReviewService.generateRiskReview(contractText, userFocus);

        reporter.updateProgress(50);
        try {
            String requirement = llmService.call(
                    buildSystemPrompt(),
                    buildUserPrompt(contractText, userFocus, riskReviewReport)
            ).trim();
            if (isBlank(requirement)) {
                throw new IllegalStateException("AI did not generate modification requirements");
            }

            reporter.updateProgress(60);
            return new DocxReviewSuggestionResult(riskReviewReport, requirement);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate modification requirements", e);
        }
    }

    protected String buildSystemPrompt() {
        return """
                你负责把法律合同风险提示转化为 DOCX 修改管线可执行的中文修改要求。
                当前修改立场：%s。
                立场优先级：
                %s

                生成规则：
                1. 必须以“法律风险提示”中的合同类型识别、对甲方不利、对乙方不利、双方共同风险为依据。
                2. 当前立场的不利风险优先转化为修改要求；相对方明显不利但会影响效力、履约或谈判稳定的风险，也要转化为平衡性修改要求。
                3. 输出中文修改要求，不要输出 Markdown 代码块，不要解释分析过程。
                4. 最多输出 8 条可执行修改。
                5. 每条修改都必须引用合同原文中真实存在的锚点片段，便于后续代码定位段落。
                6. 明确说明在该锚点附近“补充、替换、细化或删除”什么内容。
                7. 不得虚构金额、日期、比例、期限或主体名称；缺失时只能要求补充或明确。
                8. 优先关注付款、范围、交付、验收、违约、解除、保密、知识产权、责任分配、配合义务、争议解决。

                推荐格式：
                1. Anchor: "...". Risk: ... Change: ...
                2. Anchor: "...". Risk: ... Change: ...
                """.formatted(getPerspective().getPromptLabel(), buildPerspectiveInstruction());
    }

    protected String buildUserPrompt(String contractText, String userFocus, String riskReviewReport) {
        String normalizedUserFocus = isBlank(userFocus)
                ? "无额外用户关注点，请严格按当前立场和法律风险提示生成修改要求。"
                : userFocus.trim();
        return """
                用户关注点：
                %s

                法律风险提示：
                %s

                合同文本：
                %s
                """.formatted(normalizedUserFocus, riskReviewReport, contractText);
    }

    protected abstract String buildPerspectiveInstruction();

    private String extractContractText(Path inputPath) {
        try (InputStream inputStream = Files.newInputStream(inputPath);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder text = new StringBuilder();

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String paragraphText = normalizeText(paragraph.getText());
                if (!paragraphText.isEmpty()) {
                    text.append(paragraphText).append("\n\n");
                }
            }

            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    List<String> cells = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = normalizeText(cell.getText());
                        if (!cellText.isEmpty()) {
                            cells.add(cellText);
                        }
                    }
                    if (!cells.isEmpty()) {
                        text.append(String.join(" | ", cells)).append("\n");
                    }
                }
                text.append("\n");
            }

            String contractText = text.toString().trim();
            if (contractText.length() > MAX_CONTRACT_TEXT_LENGTH) {
                return contractText.substring(0, MAX_CONTRACT_TEXT_LENGTH);
            }
            return contractText;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read DOCX contract text: " + inputPath, e);
        }
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace('\u00A0', ' ')
                .replace('\u3000', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
