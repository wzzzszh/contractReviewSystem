package com.szh.contractReviewSystem.service.docx;

import com.szh.contractReviewSystem.agent.docx.DocxSkillAgentProperties;
import com.szh.contractReviewSystem.config.ArkConfig;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class LegalContractRiskReviewService {

    private static final int MAX_CONTRACT_TEXT_LENGTH = 16000;
    private static final int MAX_SKILL_TEXT_LENGTH = 9000;
    private static final int MAX_KNOWLEDGE_TEXT_LENGTH = 12000;
    private static final int MAX_RISK_REPORT_LENGTH = 12000;
    private static final int MAX_SELECTED_KNOWLEDGE_FILES = 6;

    private static final List<String> MATCH_KEYWORDS = List.of(
            "租赁", "买卖", "施工", "服务", "委托", "承揽", "物业", "培训", "建设",
            "用地", "医疗美容", "购售电", "并网调度", "劳动", "保密", "知识产权",
            "装饰装修", "机动车", "房屋", "商品房", "存量房", "电梯", "家具", "供餐"
    );

    private final DocxSkillAgentProperties properties;
    private final ArkConfig arkConfig;

    public LegalContractRiskReviewService(DocxSkillAgentProperties properties, ArkConfig arkConfig) {
        this.properties = properties;
        this.arkConfig = arkConfig;
    }

    public String generateRiskReview(String contractText, String userFocus) {
        // 1. 解析大模型配置，用于生成风险提示报告。
        ResolvedConfig config = resolveConfig();
        ArkService arkService = ArkService.builder()
                .baseUrl(config.baseUrl())
                .apiKey(config.apiKey())
                .build();
        try {
            List<ChatMessage> messages = new ArrayList<>();

            // 2. system prompt 固定风险审查规则和输出结构。
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content(buildSystemPrompt())
                    .build());

            // 3. user prompt 注入合同文本、用户关注点、.trae 技能规则和命中的知识内容。
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content(buildUserPrompt(contractText, userFocus))
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

            String report = limit(result.toString().trim(), MAX_RISK_REPORT_LENGTH);
            if (isBlank(report)) {
                throw new IllegalStateException("AI did not generate legal risk review report");
            }

            // 4. 输出结构化风险提示，供下一步转成 DOCX 修改要求。
            return report;
        } finally {
            arkService.shutdownExecutor();
        }
    }

    private String buildSystemPrompt() {
        return """
                你是法律合同风险提示技能的运行时执行器。请严格按用户提供的 `.trae/skills/legal-contract-risk` 工作方法审查合同。

                输出要求：
                1. 先识别合同类型、交易场景、地域要素和行业属性。
                2. 根据知识文件清单和命中的知识内容输出风险提示；无法准确匹配时，回退通用合同审查框架。
                3. 风险必须分为“对甲方不利”“对乙方不利”“双方共同风险”三个部分。
                4. 每个风险点必须包含：风险等级、条款位置或锚点、问题描述、利害关系、调整方向。
                5. 不直接改写完整合同条文，不虚构金额、期限、比例、日期或主体名称。
                6. 输出中文 Markdown，内容要能被后续 DOCX 修改要求生成步骤直接使用。

                必须使用的输出格式：
                # 合同风险提示

                ## 合同类型识别
                [合同类型、交易场景、地域/行业属性；不确定时明确说明]

                ## 审查依据
                - [实际参考的技能规则、知识文件或通用框架]

                ## 对甲方不利
                | 风险等级 | 条款位置或锚点 | 问题描述 | 利害关系 | 调整方向 |
                |----------|----------------|----------|----------|----------|

                ## 对乙方不利
                | 风险等级 | 条款位置或锚点 | 问题描述 | 利害关系 | 调整方向 |
                |----------|----------------|----------|----------|----------|

                ## 双方共同风险
                | 风险等级 | 条款位置或锚点 | 问题描述 | 利害关系 | 调整方向 |
                |----------|----------------|----------|----------|----------|

                ## 优先修改方向
                - [最多 8 条，必须来自上面的风险提示]
                """;
    }

    private String buildUserPrompt(String contractText, String userFocus) {
        String normalizedContractText = limit(isBlank(contractText) ? "" : contractText.trim(), MAX_CONTRACT_TEXT_LENGTH);
        String normalizedUserFocus = isBlank(userFocus) ? "无额外关注点。" : userFocus.trim();
        SkillPack skillPack = loadSkillPack(normalizedContractText + "\n" + normalizedUserFocus);
        return """
                用户关注点：
                %s

                `.trae/skills/legal-contract-risk/SKILL.md` 摘要：
                %s

                知识文件清单：
                %s

                命中的知识内容：
                %s

                合同文本：
                %s
                """.formatted(
                normalizedUserFocus,
                skillPack.skillInstructions(),
                skillPack.knowledgeIndex(),
                skillPack.selectedKnowledge(),
                normalizedContractText
        );
    }

    private SkillPack loadSkillPack(String matchingText) {
        // 读取 .trae/skills/legal-contract-risk 下的技能说明和知识库。
        Path skillDirectory = resolveLegalContractRiskSkillPath();
        Path skillFile = skillDirectory.resolve("SKILL.md");
        Path knowledgeDirectory = skillDirectory.resolve("knowledge");

        String skillInstructions = readIfExists(skillFile);
        String knowledgeIndex = buildKnowledgeIndex(knowledgeDirectory);

        // 根据合同文本和用户关注点粗匹配知识文件，只把相关内容放进模型上下文。
        String selectedKnowledge = buildSelectedKnowledge(knowledgeDirectory, matchingText);
        if (isBlank(skillInstructions)) {
            skillInstructions = "未读取到 SKILL.md，请按通用合同审查框架进行风险提示。";
        }
        if (isBlank(knowledgeIndex)) {
            knowledgeIndex = "未读取到 knowledge 文件清单。";
        }
        if (isBlank(selectedKnowledge)) {
            selectedKnowledge = "未命中具体知识文件，请回退到 SKILL.md 的通用合同审查框架。";
        }
        return new SkillPack(
                limit(skillInstructions, MAX_SKILL_TEXT_LENGTH),
                knowledgeIndex,
                limit(selectedKnowledge, MAX_KNOWLEDGE_TEXT_LENGTH)
        );
    }

    private Path resolveLegalContractRiskSkillPath() {
        // 优先使用配置项；若启动目录变化导致相对路径失效，则向上查找项目根目录下的 .trae 技能。
        String configured = firstNonBlank(
                properties.getLegalContractRiskSkillPath(),
                System.getProperty("docx.agent.legal-contract-risk-skill-path"),
                System.getenv("LEGAL_CONTRACT_RISK_SKILL_PATH")
        );
        if (isBlank(configured)) {
            configured = ".trae/skills/legal-contract-risk";
        }
        Path configuredPath = Path.of(configured).toAbsolutePath().normalize();
        if (Files.exists(configuredPath)) {
            return configuredPath;
        }

        Path current = Path.of("").toAbsolutePath().normalize();
        for (int i = 0; i < 6 && current != null; i++) {
            Path candidate = current.resolve(".trae").resolve("skills").resolve("legal-contract-risk").normalize();
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return configuredPath;
    }

    private String buildKnowledgeIndex(Path knowledgeDirectory) {
        if (!Files.isDirectory(knowledgeDirectory)) {
            return "";
        }
        try (Stream<Path> stream = Files.list(knowledgeDirectory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> "- " + path.getFileName())
                    .sorted()
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "";
        }
    }

    private String buildSelectedKnowledge(Path knowledgeDirectory, String matchingText) {
        if (!Files.isDirectory(knowledgeDirectory)) {
            return "";
        }
        String normalizedMatchingText = normalizeForMatch(matchingText);
        try (Stream<Path> stream = Files.list(knowledgeDirectory)) {
            // 给每个知识文件打分，优先选择与合同文本最接近的专项合同类型。
            List<ScoredKnowledgeFile> selectedFiles = stream
                    .filter(Files::isRegularFile)
                    .map(path -> new ScoredKnowledgeFile(path, scoreKnowledgeFile(path, normalizedMatchingText)))
                    .filter(scored -> scored.score() > 0)
                    .sorted(Comparator.comparingInt(ScoredKnowledgeFile::score).reversed())
                    .limit(MAX_SELECTED_KNOWLEDGE_FILES)
                    .toList();

            StringBuilder knowledge = new StringBuilder();
            for (ScoredKnowledgeFile selectedFile : selectedFiles) {
                String content = readIfExists(selectedFile.path());
                if (!isBlank(content)) {
                    knowledge.append("\n\n=== ")
                            .append(selectedFile.path().getFileName())
                            .append(" ===\n")
                            .append(content.trim());
                }
                if (knowledge.length() >= MAX_KNOWLEDGE_TEXT_LENGTH) {
                    break;
                }
            }

            // 返回命中的专项知识；如果没有命中，调用方会回退到通用审查框架。
            return limit(knowledge.toString().trim(), MAX_KNOWLEDGE_TEXT_LENGTH);
        } catch (Exception e) {
            return "";
        }
    }

    private int scoreKnowledgeFile(Path path, String normalizedMatchingText) {
        String fileName = path.getFileName().toString();
        String normalizedFileName = normalizeForMatch(fileName)
                .replaceFirst("^\\d+", "")
                .replace("md", "");
        int score = 0;
        if (!normalizedFileName.isEmpty() && normalizedMatchingText.contains(normalizedFileName)) {
            score += 100;
        }
        for (String keyword : MATCH_KEYWORDS) {
            String normalizedKeyword = normalizeForMatch(keyword);
            if (normalizedFileName.contains(normalizedKeyword) && normalizedMatchingText.contains(normalizedKeyword)) {
                score += 20;
            }
        }
        if (normalizedFileName.contains("租赁") && normalizedMatchingText.contains("出租")) {
            score += 15;
        }
        if (normalizedFileName.contains("买卖") && normalizedMatchingText.contains("价款")) {
            score += 10;
        }
        if (normalizedFileName.contains("施工") && normalizedMatchingText.contains("工程")) {
            score += 10;
        }
        return score;
    }

    private String readIfExists(Path path) {
        if (path == null || !Files.exists(path)) {
            return "";
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
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

    private String normalizeForMatch(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replaceAll("\\s+", "")
                .replaceAll("[\\p{Punct}，。；：、“”‘’（）【】《》]+", "")
                .toLowerCase(Locale.ROOT);
    }

    private String limit(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
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

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record SkillPack(
            String skillInstructions,
            String knowledgeIndex,
            String selectedKnowledge
    ) {
    }

    private record ScoredKnowledgeFile(Path path, int score) {
    }

    private record ResolvedConfig(String apiKey, String model, String baseUrl) {
    }
}
