package com.szh.contractReviewSystem.agent.docx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.szh.contractReviewSystem.config.ArkConfig;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.skills.FileSystemSkill;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.Skills;
import dev.langchain4j.skills.shell.RunShellCommandToolConfig;
import dev.langchain4j.skills.shell.ShellSkills;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocxSkillAgentService {

    private static final String WORD_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final int MAX_SOURCE_TEXT_LENGTH = 16000;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DocxSkillAgentProperties properties;
    private final ArkConfig arkConfig;
    private final DocxFileTools docxFileTools = new DocxFileTools();

    public DocxSkillAgentService(DocxSkillAgentProperties properties, ArkConfig arkConfig) {
        this.properties = properties;
        this.arkConfig = arkConfig;
    }

    /**
     * 实验性的基于工具的方法
     * 仅用于手动探索，主工作流应使用 modifyDocument() 中的 Java 编排 docx 技能脚本
     */
    public String execute(String task) {
        if (isBlank(task)) {
            throw new IllegalArgumentException("Agent task must not be blank");
        }
        String response = buildToolAgent().execute(task.trim());
        return isBlank(response) ? null : response.trim();
    }

    /**
     * 稳定的主路径：
     * 1. Java 直接运行 docx 技能的解压/打包脚本
     * 2. AI 仅生成一个小型的 JSON 补丁方案
     * 3. Java 修补 document.xml 并使用技能重新打包
     */
    public String modifyDocument(Path inputDocument, Path outputDocument, String modificationRequirement) {
        if (inputDocument == null) {
            throw new IllegalArgumentException("Input document must not be null");
        }
        if (outputDocument == null) {
            throw new IllegalArgumentException("Output document must not be null");
        }
        if (isBlank(modificationRequirement)) {
            throw new IllegalArgumentException("Modification requirement must not be blank");
        }

        Path normalizedInput = inputDocument.toAbsolutePath().normalize();
        Path normalizedOutput = outputDocument.toAbsolutePath().normalize();
        if (!Files.exists(normalizedInput)) {
            throw new IllegalArgumentException("Input document does not exist: " + normalizedInput);
        }

        verifyPythonEnvironment();

        try {
            Path outputParent = normalizedOutput.getParent();
            if (outputParent != null) {
                Files.createDirectories(outputParent);
            }

            Path runDirectory = createRunDirectory(outputParent);
            Path unpackedDirectory = runDirectory.resolve("unpacked");
            Path documentXml = unpackedDirectory.resolve("word").resolve("document.xml");
            Path patchPlanFile = runDirectory.resolve("patch-plan.json");

            unpackDocx(normalizedInput, unpackedDirectory);
            if (!Files.exists(documentXml)) {
                throw new IllegalStateException("document.xml not found after unpack: " + documentXml);
            }

            String sourceText = extractParagraphTextFromDocumentXml(documentXml);
            PatchPlan patchPlan = generatePatchPlan(sourceText, modificationRequirement.trim());
            savePatchPlan(patchPlanFile, patchPlan);
            applyPatchPlan(documentXml, patchPlan);
            packDocx(unpackedDirectory, normalizedInput, normalizedOutput);

            return "Document revised with docx skill pipeline. Output file: "
                    + normalizedOutput
                    + " . Patch plan: "
                    + patchPlanFile
                    + " . Review recommended.";
        } catch (IOException e) {
            throw new IllegalStateException("Failed to modify the document with docx skill pipeline", e);
        }
    }

    /**
     * 创建工作目录
     */
    private Path createRunDirectory(Path outputParent) throws IOException {
        Path baseDirectory = outputParent != null
                ? outputParent.resolve("docx-agent-work")
                : Path.of(System.getProperty("java.io.tmpdir"), "docx-agent-work");
        Path runDirectory = baseDirectory.resolve(UUID.randomUUID().toString());
        Files.createDirectories(runDirectory);
        return runDirectory;
    }

    /**
     * 使用 Python 脚本解压 docx 文件
     */
    private void unpackDocx(Path inputDocument, Path unpackedDirectory) {
        Path skillPath = resolveSkillPath();
        Path unpackScript = skillPath.resolve("scripts").resolve("office").resolve("unpack.py");
        ProcessResult result = runProcess(
                List.of(
                        properties.getPythonCommand(),
                        unpackScript.toString(),
                        inputDocument.toString(),
                        unpackedDirectory.toString()
                ),
                resolveWorkingDirectory(skillPath)
        );
        if (result.exitCode() != 0) {
            throw new IllegalStateException("Failed to unpack docx: " + result.output());
        }
    }

    /**
     * 使用 Python 脚本打包 docx 文件，带有验证回退机制
     */
    private void packDocx(Path unpackedDirectory, Path originalInput, Path outputDocument) {
        Path skillPath = resolveSkillPath();
        Path packScript = skillPath.resolve("scripts").resolve("office").resolve("pack.py");

        ProcessResult validateResult = runProcess(
                List.of(
                        properties.getPythonCommand(),
                        packScript.toString(),
                        unpackedDirectory.toString(),
                        outputDocument.toString(),
                        "--original",
                        originalInput.toString()
                ),
                resolveWorkingDirectory(skillPath)
        );
        if (validateResult.exitCode() == 0) {
            return;
        }

        ProcessResult fallbackResult = runProcess(
                List.of(
                        properties.getPythonCommand(),
                        packScript.toString(),
                        unpackedDirectory.toString(),
                        outputDocument.toString(),
                        "--original",
                        originalInput.toString(),
                        "--validate",
                        "false"
                ),
                resolveWorkingDirectory(skillPath)
        );
        if (fallbackResult.exitCode() != 0) {
            throw new IllegalStateException(
                    "Failed to pack docx. validate=true output: "
                            + validateResult.output()
                            + " ; validate=false output: "
                            + fallbackResult.output()
            );
        }
    }

    /**
     * 从 document.xml 中提取段落文本
     */
    private String extractParagraphTextFromDocumentXml(Path documentXml) {
        try {
            Document document = readXml(documentXml);
            StringBuilder text = new StringBuilder();
            for (Element paragraph : getParagraphElements(document)) {
                String paragraphText = extractParagraphText(paragraph);
                if (!isBlank(paragraphText)) {
                    text.append(paragraphText).append("\n\n");
                }
            }
            String sourceText = text.toString().trim();
            if (sourceText.length() > MAX_SOURCE_TEXT_LENGTH) {
                return sourceText.substring(0, MAX_SOURCE_TEXT_LENGTH);
            }
            return sourceText;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to extract text from document.xml", e);
        }
    }

    /**
     * 调用 LLM 生成补丁方案
     */
    private PatchPlan generatePatchPlan(String sourceText, String modificationRequirement) {
        ResolvedConfig config = resolveConfig();
        ArkService arkService = ArkService.builder()
                .baseUrl(config.baseUrl())
                .apiKey(config.apiKey())
                .build();
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content(buildPatchPlanSystemPromptFixed())
                    .build());
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content(buildPatchPlanUserPromptFixed(sourceText, modificationRequirement))
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

            String raw = result.toString().trim();
            if (isBlank(raw)) {
                throw new IllegalStateException("LLM returned empty patch plan");
            }

            String json = extractJsonObject(raw);
            PatchPlan patchPlan = OBJECT_MAPPER.readValue(json, PatchPlan.class);
            if (patchPlan == null || patchPlan.operations == null) {
                return new PatchPlan();
            }
            return patchPlan;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate patch plan", e);
        } finally {
            arkService.shutdownExecutor();
        }
    }




    /**
     * 构建补丁方案的系统提示词（固定版）
     */
    private String buildPatchPlanSystemPromptFixed() {
        return """
                You generate minimal JSON patch plans for existing Chinese contracts.
                Do not rewrite the whole contract.
                Return JSON only. No markdown. No explanation.

                Allowed operation types:
                1. replace_text_in_paragraph
                   fields: type, anchor, find, replace
                2. append_sentence_to_paragraph
                   fields: type, anchor, append
                3. insert_paragraph_after
                   fields: type, anchor, text

                Rules:
                1. anchor must be a real, unique fragment from the original contract text.
                2. Prefer replace_text_in_paragraph or append_sentence_to_paragraph.
                3. Make only the minimum necessary edits.
                4. Do not invent concrete amounts, dates, or percentages unless explicitly requested.
                5. Return at most 8 operations.

                Output schema:
                {
                  "operations": [
                    {
                      "type": "append_sentence_to_paragraph",
                      "anchor": "unique fragment from source text",
                      "append": "new sentence to append"
                    }
                  ]
                }
                """;
    }

    /**
     * 构建补丁方案的用户提示词（固定版）
     */
    private String buildPatchPlanUserPromptFixed(String sourceText, String modificationRequirement) {
        return """
                Modification requirements:
                %s

                Original contract text:
                %s
                """.formatted(modificationRequirement, sourceText);
    }

    /**
     * 保存补丁方案到 JSON 文件
     */
    private void savePatchPlan(Path patchPlanFile, PatchPlan patchPlan) throws IOException {
        Files.createDirectories(patchPlanFile.getParent());
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(patchPlanFile.toFile(), patchPlan);
    }

    /**
     * 将补丁方案应用到 document.xml
     */
    private void applyPatchPlan(Path documentXml, PatchPlan patchPlan) {
        try {
            if (patchPlan == null || patchPlan.operations == null || patchPlan.operations.isEmpty()) {
                return;
            }

            Document document = readXml(documentXml);
            for (PatchOperation operation : patchPlan.operations) {
                if (operation == null || isBlank(operation.type)) {
                    continue;
                }
                applyOperation(document, operation);
            }
            writeXml(documentXml, document);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to apply patch plan to document.xml", e);
        }
    }

    /**
     * 应用单个补丁操作
     */
    private void applyOperation(Document document, PatchOperation operation) {
        Element paragraph = findParagraph(document, operation.anchor, operation.find);
        if (paragraph == null) {
            throw new IllegalStateException("Cannot find target paragraph for anchor: " + operation.anchor);
        }

        String type = operation.type.trim();
        if ("replace_text_in_paragraph".equals(type)) {
            applyReplaceTextInParagraph(paragraph, operation);
            return;
        }
        if ("append_sentence_to_paragraph".equals(type)) {
            applyAppendSentenceToParagraph(paragraph, operation);
            return;
        }
        if ("insert_paragraph_after".equals(type)) {
            applyInsertParagraphAfter(paragraph, operation);
            return;
        }
        throw new IllegalStateException("Unsupported patch operation type: " + type);
    }

    /**
     * 应用替换文本操作：在段落中查找并替换文本
     */
    private void applyReplaceTextInParagraph(Element paragraph, PatchOperation operation) {
        String currentText = extractParagraphText(paragraph);
        String find = firstNonBlank(operation.find, operation.anchor);
        String replace = operation.replace;
        if (isBlank(find) || replace == null) {
            throw new IllegalStateException("replace_text_in_paragraph requires find/anchor and replace");
        }
        if (!currentText.contains(find)) {
            throw new IllegalStateException("Target paragraph does not contain find text: " + find);
        }
        setParagraphText(paragraph, currentText.replace(find, replace));
    }

    /**
     * 应用追加句子操作：在段落末尾追加新句子
     */
    private void applyAppendSentenceToParagraph(Element paragraph, PatchOperation operation) {
        String currentText = extractParagraphText(paragraph);
        String append = operation.append;
        if (isBlank(append)) {
            throw new IllegalStateException("append_sentence_to_paragraph requires append");
        }
        if (currentText.contains(append)) {
            return;
        }
        setParagraphText(paragraph, currentText + append);
    }

    /**
     * 应用插入段落操作：在指定段落后插入新段落
     */
    private void applyInsertParagraphAfter(Element paragraph, PatchOperation operation) {
        if (isBlank(operation.text)) {
            throw new IllegalStateException("insert_paragraph_after requires text");
        }
        Element newParagraph = createParagraphLike(paragraph, operation.text);
        Node parent = paragraph.getParentNode();
        Node nextSibling = paragraph.getNextSibling();
        if (nextSibling != null) {
            parent.insertBefore(newParagraph, nextSibling);
        } else {
            parent.appendChild(newParagraph);
        }
    }

    /**
     * 查找包含指定锚点文本的段落元素
     */
    private Element findParagraph(Document document, String anchor, String fallbackFind) {
        String key = firstNonBlank(anchor, fallbackFind);
        if (isBlank(key)) {
            return null;
        }
        for (Element paragraph : getParagraphElements(document)) {
            if (extractParagraphText(paragraph).contains(key)) {
                return paragraph;
            }
        }
        return null;
    }

    /**
     * 获取文档中所有段落元素
     */
    private List<Element> getParagraphElements(Document document) {
        List<Element> paragraphs = new ArrayList<>();
        NodeList nodeList = document.getElementsByTagNameNS(WORD_NS, "p");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element element) {
                paragraphs.add(element);
            }
        }
        return paragraphs;
    }

    /**
     * 提取单个段落的纯文本内容
     */
    private String extractParagraphText(Element paragraph) {
        StringBuilder text = new StringBuilder();
        appendText(paragraph, text);
        return normalizeText(text.toString());
    }

    /**
     * 递归追加节点中的文本内容
     */
    private void appendText(Node node, StringBuilder text) {
        if (node instanceof Element element) {
            String localName = element.getLocalName();
            if ("t".equals(localName) && WORD_NS.equals(element.getNamespaceURI())) {
                text.append(element.getTextContent());
                return;
            }
            if ("tab".equals(localName) && WORD_NS.equals(element.getNamespaceURI())) {
                text.append('\t');
                return;
            }
            if (("br".equals(localName) || "cr".equals(localName)) && WORD_NS.equals(element.getNamespaceURI())) {
                text.append('\n');
                return;
            }
        }
        Node child = node.getFirstChild();
        while (child != null) {
            appendText(child, text);
            child = child.getNextSibling();
        }
    }

    /**
     * 设置段落的文本内容，保留原有样式
     */
    private void setParagraphText(Element paragraph, String newText) {
        Document document = paragraph.getOwnerDocument();
        Node paragraphProperties = findDirectChild(paragraph, "pPr");
        Node runProperties = findFirstRunProperties(paragraph);

        List<Node> toRemove = new ArrayList<>();
        Node child = paragraph.getFirstChild();
        while (child != null) {
            Node next = child.getNextSibling();
            if (child != paragraphProperties) {
                toRemove.add(child);
            }
            child = next;
        }
        for (Node node : toRemove) {
            paragraph.removeChild(node);
        }

        Element run = document.createElementNS(WORD_NS, "w:r");
        if (runProperties != null) {
            run.appendChild(runProperties.cloneNode(true));
        }

        Element text = document.createElementNS(WORD_NS, "w:t");
        if (newText.startsWith(" ") || newText.endsWith(" ")) {
            text.setAttributeNS(XMLConstants.XML_NS_URI, "xml:space", "preserve");
        }
        text.setTextContent(newText);
        run.appendChild(text);
        paragraph.appendChild(run);
    }

    /**
     * 创建与模板段落样式相同的新段落
     */
    private Element createParagraphLike(Element templateParagraph, String text) {
        Document document = templateParagraph.getOwnerDocument();
        Element paragraph = document.createElementNS(WORD_NS, "w:p");

        Node paragraphProperties = findDirectChild(templateParagraph, "pPr");
        if (paragraphProperties != null) {
            paragraph.appendChild(paragraphProperties.cloneNode(true));
        }

        Node runProperties = findFirstRunProperties(templateParagraph);
        Element run = document.createElementNS(WORD_NS, "w:r");
        if (runProperties != null) {
            run.appendChild(runProperties.cloneNode(true));
        }

        Element textNode = document.createElementNS(WORD_NS, "w:t");
        if (text.startsWith(" ") || text.endsWith(" ")) {
            textNode.setAttributeNS(XMLConstants.XML_NS_URI, "xml:space", "preserve");
        }
        textNode.setTextContent(text);
        run.appendChild(textNode);
        paragraph.appendChild(run);
        return paragraph;
    }

    /**
     * 查找元素的直接子元素
     */
    private Node findDirectChild(Element parent, String localName) {
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element element
                    && WORD_NS.equals(element.getNamespaceURI())
                    && localName.equals(element.getLocalName())) {
                return child;
            }
            child = child.getNextSibling();
        }
        return null;
    }

    /**
     * 查找段落中第一个运行属性（rPr）
     */
    private Node findFirstRunProperties(Element paragraph) {
        Node child = paragraph.getFirstChild();
        while (child != null) {
            if (child instanceof Element element
                    && WORD_NS.equals(element.getNamespaceURI())
                    && "r".equals(element.getLocalName())) {
                Node runProps = findDirectChild(element, "rPr");
                if (runProps != null) {
                    return runProps;
                }
            }
            child = child.getNextSibling();
        }
        return null;
    }

    /**
     * 读取 XML 文件
     */
    private Document readXml(Path xmlPath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try (InputStream inputStream = Files.newInputStream(xmlPath)) {
            return factory.newDocumentBuilder().parse(inputStream);
        }
    }

    /**
     * 将 Document 对象写入 XML 文件
     */
    private void writeXml(Path xmlPath, Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        try (OutputStream outputStream = Files.newOutputStream(xmlPath)) {
            transformer.transform(new DOMSource(document), new StreamResult(outputStream));
        }
    }

    /**
     * 从原始响应中提取 JSON 对象
     */
    private String extractJsonObject(String raw) {
        String trimmed = raw.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("Patch plan response is not valid JSON: " + raw);
        }
        return trimmed.substring(start, end + 1);
    }

    /**
     * 构建基于工具的 Agent
     */
    private DocxSkillAgent buildToolAgent() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("docx-agent is disabled");
        }

        Path skillPath = resolveSkillPath();
        Path skillFile = skillPath.resolve("SKILL.md");
        if (!Files.exists(skillFile)) {
            throw new IllegalStateException("Docx skill not found: " + skillFile.toAbsolutePath());
        }

        verifyPythonEnvironment();

        Path workingDirectory = resolveWorkingDirectory(skillPath);
        FileSystemSkill docxSkill = FileSystemSkillLoader.loadSkill(skillPath);
        Skills skills = Skills.from(docxSkill);
        ShellSkills shellSkills = ShellSkills.builder()
                .skills(docxSkill)
                .runShellCommandToolConfig(buildShellCommandConfig(workingDirectory))
                .build();

        return AiServices.builder(DocxSkillAgent.class)
                .chatModel(buildOpenAiChatModel())
                .systemMessage(buildToolAgentSystemMessage(skills.formatAvailableSkills(), skillPath, workingDirectory))
                .tools(docxFileTools)
                .toolProviders(skills.toolProvider(), shellSkills.toolProvider())
                .maxSequentialToolsInvocations(properties.getMaxSequentialToolsInvocations())
                .build();
    }

    /**
     * 构建 OpenAI 聊天模型
     */
    private OpenAiChatModel buildOpenAiChatModel() {
        ResolvedConfig config = resolveConfig();
        return OpenAiChatModel.builder()
                .baseUrl(config.baseUrl())
                .apiKey(config.apiKey())
                .modelName(config.model())
                .temperature(properties.getTemperature())
                .maxCompletionTokens(properties.getMaxCompletionTokens())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    /**
     * 构建工具 Agent 的系统消息
     */
    private String buildToolAgentSystemMessage(String availableSkills, Path skillPath, Path workingDirectory) {
        return """
                You are a Java-hosted DOCX editing agent.
                This path is experimental only.

                Rules:
                1. Always activate the docx skill and read its SKILL.md before editing a document.
                2. Use only absolute paths for user files.
                3. For existing .docx files, follow this workflow: unpack -> read/edit XML -> pack.
                4. Prefer reading and writing unpacked XML files with readTextFile and writeTextFile.
                5. Do not invent script names.
                6. Stop after the first concrete failure.

                Python command: %s
                docx skill path: %s
                shell working directory: %s

                Available skills:
                %s
                """.formatted(
                properties.getPythonCommand(),
                skillPath.toAbsolutePath().normalize(),
                workingDirectory.toAbsolutePath().normalize(),
                availableSkills
        );
    }

    /**
     * 解析配置信息（API密钥、模型、基础URL）
     */
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
            throw new IllegalStateException("Missing API key for docx-agent. Configure docx-agent.apiKey or ark.apiKey");
        }
        if (isBlank(model)) {
            throw new IllegalStateException("Missing model for docx-agent. Configure docx-agent.model or ark.model");
        }
        return new ResolvedConfig(apiKey, model, baseUrl);
    }

    /**
     * 构建 Shell 命令工具配置
     */
    private RunShellCommandToolConfig buildShellCommandConfig(Path workingDirectory) {
        return RunShellCommandToolConfig.builder()
                .name("run_shell_command")
                .description("Run the exact shell command required by the active skill workflow.")
                .commandParameterName("command")
                .commandParameterDescription("The full PowerShell command to execute. Prefer absolute paths for user files.")
                .timeoutSecondsParameterName("timeoutSeconds")
                .timeoutSecondsParameterDescription("How many seconds to wait before the command is interrupted.")
                .workingDirectory(workingDirectory)
                .maxStdOutChars(properties.getMaxStdOutChars())
                .maxStdErrChars(properties.getMaxStdErrChars())
                .throwToolArgumentsExceptions(true)
                .build();
    }

    /**
     * 验证 Python 环境和依赖
     */
    private void verifyPythonEnvironment() {
        ProcessResult versionResult = runProcess(List.of(properties.getPythonCommand(), "--version"), null);
        if (versionResult.exitCode() != 0) {
            throw new IllegalStateException("Python command is not usable for docx-agent: " + versionResult.output());
        }

        ProcessResult dependencyResult = runProcess(
                List.of(properties.getPythonCommand(), "-c", "import lxml.etree, defusedxml.minidom"),
                null
        );
        if (dependencyResult.exitCode() != 0) {
            throw new IllegalStateException(
                    "Python is missing required docx-agent dependencies. Install lxml and defusedxml. "
                            + dependencyResult.output()
            );
        }
    }

    /**
     * 运行外部进程
     */
    private ProcessResult runProcess(List<String> command, Path workingDirectory) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            if (workingDirectory != null) {
                processBuilder.directory(workingDirectory.toFile());
            }
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            return new ProcessResult(exitCode, output.trim());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Process execution interrupted: " + command, e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to run process: " + command, e);
        }
    }

    /**
     * 解析技能路径
     */
    private Path resolveSkillPath() {
        String configured = firstNonBlank(
                properties.getSkillPath(),
                System.getProperty("docx.agent.skill-path"),
                System.getenv("DOCX_AGENT_SKILL_PATH")
        );
        if (!isBlank(configured)) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.home"), ".codex", "skills", "docx")
                .toAbsolutePath()
                .normalize();
    }

    /**
     * 解析工作目录
     */
    private Path resolveWorkingDirectory(Path skillPath) {
        String configured = firstNonBlank(
                properties.getShellWorkingDirectory(),
                System.getProperty("docx.agent.working-directory"),
                System.getenv("DOCX_AGENT_WORKDIR")
        );
        if (isBlank(configured)) {
            return skillPath;
        }
        return Path.of(configured).toAbsolutePath().normalize();
    }

    /**
     * 标准化文本：替换特殊空格字符，压缩连续空白
     */
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

    /**
     * 获取第一个非空字符串
     */
    private static String firstNonBlank(String... values) {
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

    /**
     * 判断字符串是否为空
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 已解析的配置信息记录
     */
    private record ResolvedConfig(String apiKey, String model, String baseUrl) {
    }

    /**
     * 进程执行结果记录
     */
    private record ProcessResult(int exitCode, String output) {
    }

    /**
     * 补丁方案类
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PatchPlan {
        public List<PatchOperation> operations = new ArrayList<>();
    }

    /**
     * 补丁操作类
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PatchOperation {
        public String type;      // 操作类型
        public String anchor;    // 定位锚点
        public String find;      // 要查找的文本
        public String replace;   // 替换后的文本
        public String append;    // 要追加的文本
        public String text;      // 新段落的文本
    }
}
