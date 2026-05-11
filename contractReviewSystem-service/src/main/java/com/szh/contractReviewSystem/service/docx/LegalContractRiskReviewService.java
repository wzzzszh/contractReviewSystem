package com.szh.contractReviewSystem.service.docx;

import com.szh.contractReviewSystem.agent.docx.DocxSkillAgentProperties;
import com.szh.contractReviewSystem.llm.LLMService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
            "租赁", "买卖", "施工", "服务", "委托", "承担", "物业", "培训", "建设",
            "用地", "医疗美容", "采购", "网络调度", "劳动", "保密", "知识产权",
            "装修", "机动车", "房屋", "商品房", "存量房", "电梯", "家具", "餐饮"
    );

    private final DocxSkillAgentProperties properties;
    private final LLMService llmService;

    public LegalContractRiskReviewService(DocxSkillAgentProperties properties, LLMService llmService) {
        this.properties = properties;
        this.llmService = llmService;
    }

    public String generateRiskReview(String contractText, String userFocus) {
        try {
            String report = llmService.call(buildSystemPrompt(), buildUserPrompt(contractText, userFocus)).trim();
            report = limit(report, MAX_RISK_REPORT_LENGTH);
            if (isBlank(report)) {
                throw new IllegalStateException("AI did not generate legal risk review report");
            }
            return report;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate legal risk review report", e);
        }
    }

    private String buildSystemPrompt() {
        return """
                你是法律合同风险提示引擎。请严格按用户提供的 `.trae/skills/legal-contract-risk` 工作方法审查合同。
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
                [合同类型、交易场景、地域、行业属性；不确定时明确说明]

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
        Path skillDirectory = resolveLegalContractRiskSkillPath();
        Path skillFile = skillDirectory.resolve("SKILL.md");
        Path knowledgeDirectory = skillDirectory.resolve("knowledge");

        String skillInstructions = readIfExists(skillFile);
        String knowledgeIndex = buildKnowledgeIndex(knowledgeDirectory);
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

    private String normalizeForMatch(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replaceAll("\\s+", "")
                .replaceAll("[\\p{Punct}，。；：、“”‘’（）【】《》…]+", "")
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

    private record SkillPack(String skillInstructions, String knowledgeIndex, String selectedKnowledge) {
    }

    private record ScoredKnowledgeFile(Path path, int score) {
    }
}
