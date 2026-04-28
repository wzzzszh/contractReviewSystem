package com.szh.contractReviewSystem.agent.docx;

import com.szh.contractReviewSystem.config.ArkConfig;
import com.szh.contractReviewSystem.llm.ark.ArkConfigLoader;
import com.szh.contractReviewSystem.testsupport.TestResourceFiles;

import java.nio.file.Path;

public class DocxSkillAgentExample {

    private static final String DEFAULT_WINDOWS_PYTHON = "D:\\Tools\\python.exe";
    private static final String SAMPLE_DOCX = "劳动合同（word范本）.docx";
    private static final String OUTPUT_DOCX = "劳动合同（word范本）-甲方修订版.docx";

    public static void main(String[] args) {
        DocxSkillAgentProperties properties = new DocxSkillAgentProperties();
        properties.setApiKey(firstNonBlank(
                System.getProperty("ark.api-key"),
                System.getenv("ARK_API_KEY"),
                safeGetApiKey()
        ));
        properties.setModel(firstNonBlank(
                System.getProperty("ark.model"),
                System.getenv("ARK_MODEL"),
                safeGetModel()
        ));
        properties.setSkillPath(firstNonBlank(
                System.getProperty("docx.agent.skill-path"),
                System.getenv("DOCX_AGENT_SKILL_PATH")
        ));
        properties.setPythonCommand(firstNonBlank(
                System.getProperty("docx.agent.python-command"),
                System.getenv("DOCX_AGENT_PYTHON"),
                DEFAULT_WINDOWS_PYTHON
        ));

        ArkConfig arkConfig = new ArkConfig();
        arkConfig.setApiKey(properties.getApiKey());
        arkConfig.setModel(properties.getModel());

        DocxSkillAgentService agentService = new DocxSkillAgentService(properties, arkConfig);

        Path input = TestResourceFiles.require(SAMPLE_DOCX)
                .toPath()
                .toAbsolutePath()
                .normalize();
        Path output = resolveModuleTargetPath(input, OUTPUT_DOCX);
        // 调用 agent 修改文档,翻译：从甲方视角审阅本劳动合同。
        // 强化关于岗位调整、保密、违约责任及合同解除的条款。
        // 对表述模糊或易引发争议的措辞进行优化，但不杜撰具体金额、日期或比例。
        // 尽量保留原有的标题结构与格式。
        DocxSkillAgentService.ModifyDocumentResult result = agentService.modifyDocument(
                input,
                output,
                """
                Review this labor contract from Party A's perspective.
                Strengthen the clauses about job adjustment, confidentiality, breach liability, and termination.
                Refine vague or dispute-prone wording, but do not invent specific amounts, dates, or percentages.
                Keep the original heading structure and formatting as much as possible.
                """
        );

        System.out.println(result == null ? "Agent 没有返回文本。 " : result.message());

    }

    private static String safeGetApiKey() {
        try {
            return ArkConfigLoader.getApiKey();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String safeGetModel() {
        try {
            return ArkConfigLoader.getModel();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static Path resolveModuleTargetPath(Path input, String fileName) {
        Path moduleRoot = input.getParent()
                .getParent()
                .getParent();
        return moduleRoot.resolve("target")
                .resolve("docx-agent-output")
                .resolve(fileName)
                .toAbsolutePath()
                .normalize();
    }
}
