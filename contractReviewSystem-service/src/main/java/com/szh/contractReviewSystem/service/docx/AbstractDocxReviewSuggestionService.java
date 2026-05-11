package com.szh.contractReviewSystem.service.docx;

import com.szh.contractReviewSystem.agent.docx.DocxSkillAgentProperties;
import com.szh.contractReviewSystem.config.ArkConfig;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
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

    private final DocxSkillAgentProperties properties;
    private final ArkConfig arkConfig;
    private final LegalContractRiskReviewService legalContractRiskReviewService;

    protected AbstractDocxReviewSuggestionService(DocxSkillAgentProperties properties,
                                                  ArkConfig arkConfig,
                                                  LegalContractRiskReviewService legalContractRiskReviewService) {
        this.properties = properties;
        this.arkConfig = arkConfig;
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
        // 1. 从 DOCX 中抽取纯文本，供风险审查和修改要求生成共同使用。
        reporter.updateProgress(20);
        String contractText = extractContractText(inputPath);

        // 2. 先按 .trae 法律合同风险技能生成结构化风险提示。
        reporter.updateProgress(35);
        String riskReviewReport = legalContractRiskReviewService.generateRiskReview(contractText, userFocus);

        // 3. 再把风险提示、合同原文、用户关注点一起交给大模型，生成可执行修改要求。
        reporter.updateProgress(50);
        ResolvedConfig config = resolveConfig();
        ArkService arkService = ArkService.builder()
                .baseUrl(config.baseUrl())
                .apiKey(config.apiKey())
                .build();
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content(buildSystemPrompt())
                    .build());
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content(buildUserPrompt(contractText, userFocus, riskReviewReport))
                    .build());

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(config.model())
                    .messages(messages)
                    .build();

            StringBuilder result = new StringBuilder();
            arkService.createChatCompletion(request)
                    .getChoices()
                    .forEach(choice -> {
                        if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                            result.append(choice.getMessage().getContent());
                        }
                    });

            String requirement = result.toString().trim();
            if (isBlank(requirement)) {
                throw new IllegalStateException("AI did not generate modification requirements");
            }

            // 4. 同时返回风险提示报告和最终修改要求，后续补丁管线只使用修改要求。
            reporter.updateProgress(60);
            return new DocxReviewSuggestionResult(riskReviewReport, requirement);
        } finally {
            arkService.shutdownExecutor();
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
                7. 不得虚构金额、日期、比例、期限、主体名称；缺失时只能要求补充或明确。
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

            // 先提取普通段落文本。
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String paragraphText = normalizeText(paragraph.getText());
                if (!paragraphText.isEmpty()) {
                    text.append(paragraphText).append("\n\n");
                }
            }

            // 再提取表格文本；单元格用竖线分隔，尽量保留表格中的语义关系。
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

            // 控制输入长度，避免一次请求超过模型上下文。
            String contractText = text.toString().trim();
            if (contractText.length() > MAX_CONTRACT_TEXT_LENGTH) {
                return contractText.substring(0, MAX_CONTRACT_TEXT_LENGTH);
            }
            return contractText;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read DOCX contract text: " + inputPath, e);
        }
    }

    private ResolvedConfig resolveConfig() {
        String apiKey = firstNonBlank(
                properties.getApiKey(),
                arkConfig.getApiKey(),
                System.getProperty("ark.api-key"),
                System.getenv("ARK_API_KEY")
        );
        String model = firstNonBlank(
                properties.getModel(),
                arkConfig.getModel(),
                System.getProperty("ark.model"),
                System.getenv("ARK_MODEL")
        );
        String baseUrl = firstNonBlank(
                properties.getBaseUrl(),
                arkConfig.getBaseUrl(),
                "https://ark.cn-beijing.volces.com/api/v3"
        );

        if (isBlank(apiKey)) {
            throw new IllegalStateException("Missing Ark API key. Configure docx-agent.apiKey or ark.apiKey");
        }
        if (isBlank(model)) {
            throw new IllegalStateException("Missing Ark model. Configure docx-agent.model or ark.model");
        }
        return new ResolvedConfig(apiKey, model, baseUrl);
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

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record ResolvedConfig(String apiKey, String model, String baseUrl) {
    }
}
