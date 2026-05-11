package com.szh.contractReviewSystem.agent.docx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.szh.contractReviewSystem.config.FileLifecycleProperties;
import com.szh.contractReviewSystem.llm.LLMService;
import com.szh.contractReviewSystem.llm.LlmEndpointResolver;
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
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DocxSkillAgentService {

    private static final String WORD_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final int MAX_SOURCE_TEXT_LENGTH = 16000;
    private static final double PATCH_APPLY_MIN_SUCCESS_RATIO = 0.9D;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DocxSkillAgentProperties properties;
    private final LLMService llmService;
    private final LlmEndpointResolver llmEndpointResolver;
    private final FileLifecycleProperties fileLifecycleProperties;
    private final DocxFileTools docxFileTools = new DocxFileTools();

    public DocxSkillAgentService(DocxSkillAgentProperties properties,
                                 LLMService llmService,
                                 LlmEndpointResolver llmEndpointResolver,
                                 FileLifecycleProperties fileLifecycleProperties) {
        this.properties = properties;
        this.llmService = llmService;
        this.llmEndpointResolver = llmEndpointResolver;
        this.fileLifecycleProperties = fileLifecycleProperties;
    }
    /**
      * 说明：该方法注释已修复（原注释出现乱码）。
      * 说明：该方法注释已修复（原注释出现乱码）。
     */
    public String execute(String task) {
        if (isBlank(task)) {
            throw new IllegalArgumentException("Agent task must not be blank");
        }
        String response = buildToolAgent().execute(task.trim());
        return isBlank(response) ? null : response.trim();
    }

    /**
      * 说明：该方法注释已修复（原注释出现乱码）。
      * 说明：该方法注释已修复（原注释出现乱码）。
      * 说明：该方法注释已修复（原注释出现乱码）。
      * 说明：该方法注释已修复（原注释出现乱码）。
     */
    public ModifyDocumentResult modifyDocument(Path inputDocument, Path outputDocument, String modificationRequirement) {
        if (inputDocument == null) {
            throw new IllegalArgumentException("输入的文档不能为空");
        }
        if (outputDocument == null) {
            throw new IllegalArgumentException("输出的文档不能为空");
        }
        if (isBlank(modificationRequirement)) {
            throw new IllegalArgumentException("修改要求不能为空");
        }
        // 说明：步骤注释已修复（原注释出现乱码）。
        Path normalizedInput = inputDocument.toAbsolutePath().normalize();
        Path normalizedOutput = outputDocument.toAbsolutePath().normalize();
        if (!Files.exists(normalizedInput)) {
            throw new IllegalArgumentException("输入的文档不存在: " + normalizedInput);
        }

        // 说明：步骤注释已修复（原注释出现乱码）。
        verifyPythonEnvironment();

        try {
            // 说明：步骤注释已修复（原注释出现乱码）。
            Path outputParent = normalizedOutput.getParent();
            if (outputParent != null) {
                Files.createDirectories(outputParent);
            }

            // 说明：步骤注释已修复（原注释出现乱码）。
            Path runDirectory = createRunDirectory(outputParent);
            Path unpackedDirectory = runDirectory.resolve("unpacked");
            Path documentXml = unpackedDirectory.resolve("word").resolve("document.xml");
            Path patchPlanFile = runDirectory.resolve("patch-plan.json");

            // 说明：步骤注释已修复（原注释出现乱码）。
            unpackDocx(normalizedInput, unpackedDirectory);
            if (!Files.exists(documentXml)) {
                throw new IllegalStateException("document.xml 在解压后不存在: " + documentXml);
            }

            // 说明：步骤注释已修复（原注释出现乱码）。
            String sourceText = extractParagraphTextFromDocumentXml(documentXml);

            // 说明：步骤注释已修复（原注释出现乱码）。
            PatchExecutionResult patchExecutionResult = applyPatchPlanWithRetry(
                    runDirectory,
                    documentXml,
                    patchPlanFile,
                    sourceText,
                    modificationRequirement.trim()
            );
            PatchApplyResult patchApplyResult = patchExecutionResult.patchApplyResult();

            // 说明：步骤注释已修复（原注释出现乱码）。
            packDocx(unpackedDirectory, normalizedInput, normalizedOutput);

            return new ModifyDocumentResult(
                    normalizedOutput,
                    runDirectory,
                    patchExecutionResult.finalPatchPlanFile(),
                    buildResultMessage(normalizedOutput, patchExecutionResult.finalPatchPlanFile(), patchApplyResult),
                    patchApplyResult.appliedCount(),
                    patchApplyResult.skippedOperationMessages().size(),
                    patchApplyResult.skippedOperationMessages(),
                    patchApplyResult.warningMessage()
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to modify the document with docx skill pipeline", e);
        }
    }

    /**
      * 说明：该方法注释已修复（原注释出现乱码）。
     */
    private Path createRunDirectory(Path outputParent) throws IOException {
        Path baseDirectory = resolveDocxAgentWorkRoot()
                .resolve(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        Path runDirectory = baseDirectory.resolve("task-" + UUID.randomUUID());
        Files.createDirectories(runDirectory);
        return runDirectory;
    }

    private Path resolveDocxAgentWorkRoot() {
        String configured = fileLifecycleProperties == null ? null : fileLifecycleProperties.getDocxAgentWorkRoot();
        if (isBlank(configured)) {
            configured = "work/docx-agent";
        }
        Path path = Path.of(configured);
        if (path.isAbsolute()) {
            return path.toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.dir"), configured).toAbsolutePath().normalize();
    }

    /**
      * 说明：该方法注释已修复（原注释出现乱码）。
     */
    private void unpackDocx(Path inputDocument, Path unpackedDirectory) {
        // 说明：步骤注释已修复（原注释出现乱码）。
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
      * 说明：该方法注释已修复（原注释出现乱码）。
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
      * 说明：该方法注释已修复（原注释出现乱码）。
     */
    private String extractParagraphTextFromDocumentXml(Path documentXml) {
        try {
            Document document = readXml(documentXml);
            StringBuilder text = new StringBuilder();
            // 说明：步骤注释已修复（原注释出现乱码）。
            for (Element paragraph : getParagraphElements(document)) {
                // 说明：步骤注释已修复（原注释出现乱码）。
                String paragraphText = extractParagraphText(paragraph);
                // 说明：步骤注释已修复（原注释出现乱码）。
                if (!isBlank(paragraphText)) {
                    text.append(paragraphText).append("\n\n");
                }
            }
            // 说明：步骤注释已修复（原注释出现乱码）。
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
      * 说明：该方法注释已修复（原注释出现乱码）。
     */
    private PatchPlan generatePatchPlan(String sourceText, String modificationRequirement) {
        return generatePatchPlanInternal(sourceText, modificationRequirement, null, null);
    }

    private PatchPlan generatePatchPlanWithFeedback(String sourceText,
                                                    String modificationRequirement,
                                                    Path previousPatchPlanFile,
                                                    PatchApplyResult previousApplyResult) {
        return generatePatchPlanInternal(sourceText, modificationRequirement, previousPatchPlanFile, previousApplyResult);
    }

    private PatchPlan generatePatchPlanInternal(String sourceText,
                                                String modificationRequirement,
                                                Path previousPatchPlanFile,
                                                PatchApplyResult previousApplyResult) {
        boolean hasFeedback = previousPatchPlanFile != null && previousApplyResult != null;
        String systemPrompt = hasFeedback
                ? buildPatchPlanRetrySystemPrompt()
                : buildPatchPlanSystemPromptFixed();
        String userPrompt = hasFeedback
                ? buildPatchPlanRetryUserPrompt(sourceText, modificationRequirement, previousPatchPlanFile, previousApplyResult)
                : buildPatchPlanUserPromptFixed(sourceText, modificationRequirement);

        try {
            String raw = llmService.call(systemPrompt, userPrompt).trim();
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
        }
    }
    /**
      * 说明：该方法注释已修复（原注释出现乱码）。
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
      * 说明：该方法注释已修复（原注释出现乱码）。
     */
    private String buildPatchPlanUserPromptFixed(String sourceText, String modificationRequirement) {
        return """
                Modification requirements:
                %s

                Original contract text:
                %s
                """.formatted(modificationRequirement, sourceText);
    }

    private String buildPatchPlanRetrySystemPrompt() {
        return """
                You are fixing a previously failed JSON patch plan for an existing Chinese contract.
                Return JSON only. No markdown. No explanation.

                Allowed operation types:
                1. replace_text_in_paragraph
                   fields: type, anchor, find, replace
                2. append_sentence_to_paragraph
                   fields: type, anchor, append
                3. insert_paragraph_after
                   fields: type, anchor, text

                Hard rules:
                1. Every anchor must be an exact fragment from source text.
                2. Remove or rewrite operations that previously failed.
                3. Keep edits minimal and executable.
                4. Return at most 8 operations.
                5. Keep output schema exactly as {"operations":[...]}.
                """;
    }

    private String buildPatchPlanRetryUserPrompt(String sourceText,
                                                 String modificationRequirement,
                                                 Path previousPatchPlanFile,
                                                 PatchApplyResult previousApplyResult) {
        String previousPlanJson = readPatchPlanFileSafe(previousPatchPlanFile);
        String skipDetails = formatSkippedMessages(previousApplyResult == null
                ? Collections.emptyList()
                : previousApplyResult.skippedOperationMessages());
        int appliedCount = previousApplyResult == null ? 0 : previousApplyResult.appliedCount();
        return """
                Modification requirements:
                %s

                Original contract text:
                %s

                Previous patch plan JSON:
                %s

                Previous apply result:
                applied_count=%d
                skipped_messages:
                %s

                Please return a repaired patch plan JSON only.
                """.formatted(
                modificationRequirement,
                sourceText,
                previousPlanJson,
                appliedCount,
                skipDetails
        );
    }

    /**
      * 说明：该方法注释已修复（原注释出现乱码）。
     */
    private void savePatchPlan(Path patchPlanFile, PatchPlan patchPlan) throws IOException {
        Files.createDirectories(patchPlanFile.getParent());
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(patchPlanFile.toFile(), patchPlan);
    }

    private PatchExecutionResult applyPatchPlanWithRetry(Path runDirectory,
                                                         Path documentXml,
                                                         Path finalPatchPlanFile,
                                                         String sourceText,
                                                         String modificationRequirement) throws IOException {
        int maxAttempts = normalizePositive(properties.getPatchPlanMaxAttempts(), 3);
        int minAppliedOperations = normalizePositive(properties.getPatchMinAppliedOperations(), 1);

        Path attemptsDirectory = runDirectory.resolve("patch-plan-attempts");
        Path baselineDocumentXml = runDirectory.resolve("baseline-document.xml");
        Files.copy(documentXml, baselineDocumentXml, StandardCopyOption.REPLACE_EXISTING);

        PatchApplyResult lastApplyResult = null;
        Path previousPatchPlanAttemptFile = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Files.copy(baselineDocumentXml, documentXml, StandardCopyOption.REPLACE_EXISTING);

            PatchPlan patchPlan = attempt == 1
                    ? generatePatchPlan(sourceText, modificationRequirement)
                    : generatePatchPlanWithFeedback(sourceText, modificationRequirement, previousPatchPlanAttemptFile, lastApplyResult);

            Path attemptPlanFile = attemptsDirectory.resolve("patch-plan-attempt-" + attempt + ".json");
            savePatchPlan(attemptPlanFile, patchPlan);

            PatchApplyResult applyResult = applyPatchPlan(documentXml, patchPlan);
            lastApplyResult = applyResult;
            previousPatchPlanAttemptFile = attemptPlanFile;

            if (isPatchApplyAcceptable(applyResult, minAppliedOperations)) {
                Files.copy(attemptPlanFile, finalPatchPlanFile, StandardCopyOption.REPLACE_EXISTING);
                return new PatchExecutionResult(applyResult, finalPatchPlanFile);
            }
        }

        if (previousPatchPlanAttemptFile != null) {
            Files.copy(previousPatchPlanAttemptFile, finalPatchPlanFile, StandardCopyOption.REPLACE_EXISTING);
        }
        throw new IllegalStateException(buildPatchRetryFailureMessage(maxAttempts, minAppliedOperations, lastApplyResult));
    }

    /**
      * 说明：该方法注释已修复（原注释出现乱码）。
     */
    private PatchApplyResult applyPatchPlan(Path documentXml, PatchPlan patchPlan) {
        try {
            if (patchPlan == null || patchPlan.operations == null || patchPlan.operations.isEmpty()) {
                return new PatchApplyResult(0, Collections.emptyList(), "No patch operations generated by AI.");
            }

            Document document = readXml(documentXml);
            int appliedCount = 0;
            List<String> appliedOperationSummaries = new ArrayList<>();
            List<String> skippedOperationMessages = new ArrayList<>();
            for (PatchOperation operation : patchPlan.operations) {
                if (operation == null || isBlank(operation.type)) {
                    continue;
                }
                try {
                    applyOperation(document, operation);
                    appliedCount++;
                    appliedOperationSummaries.add(buildDocumentSummaryLine(operation));
                } catch (IllegalStateException e) {
                    skippedOperationMessages.add(describeOperation(operation) + " ; reason: " + e.getMessage());
                }
            }
            appendModificationSummary(document, appliedOperationSummaries);
            writeXml(documentXml, document);
            String warningMessage = skippedOperationMessages.isEmpty()
                    ? null
                    : "Some patch operations were skipped. See skippedOperationMessages for details.";
            return new PatchApplyResult(appliedCount, skippedOperationMessages, warningMessage);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to apply patch plan to document.xml. " + e.getMessage(), e);
        }
    }

    /**
      * 说明：该方法注释已修复（原注释出现乱码）。
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
      * 说明：该方法注释已修复（原注释出现乱码）。
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
      * 说明：该方法注释已修复（原注释出现乱码）。
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
      * 说明：该方法注释已修复（原注释出现乱码）。
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
      * 说明：该方法注释已修复（原注释出现乱码）。
     */
    private Element findParagraph(Document document, String anchor, String fallbackFind) {
        String key = firstNonBlank(anchor, fallbackFind);
        if (isBlank(key)) {
            return null;
        }
        String normalizedKey = normalizeMatchingText(key);
        for (Element paragraph : getParagraphElements(document)) {
            String paragraphText = extractParagraphText(paragraph);
            if (paragraphText.contains(key) || normalizeMatchingText(paragraphText).contains(normalizedKey)) {
                return paragraph;
            }
        }
        return null;
    }

    /**
      * 说明：该方法注释已修复（原注释出现乱码）。
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
      * 说明：该方法注释已修复（原注释出现乱码）。
     */
    private String extractParagraphText(Element paragraph) {
        StringBuilder text = new StringBuilder();
        appendText(paragraph, text);
        return normalizeText(text.toString());
    }

    /**
      * 说明：该方法注释已修复（原注释出现乱码）。
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
      * 说明：该方法注释已修复（原注释出现乱码）。
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
      * 说明：该方法注释已修复（原注释出现乱码）。
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

    private void appendModificationSummary(Document document, List<String> appliedOperationSummaries) {
        if (appliedOperationSummaries == null || appliedOperationSummaries.isEmpty()) {
            return;
        }

        Element body = findDocumentBody(document);
        appendParagraphToBody(body, createSummaryParagraph(document, "Document modification summary", true));
        for (int i = 0; i < appliedOperationSummaries.size(); i++) {
            String summaryText = (i + 1) + ". " + appliedOperationSummaries.get(i);
            appendParagraphToBody(body, createSummaryParagraph(document, summaryText, false));
        }
    }

    private Element createSummaryParagraph(Document document, String text, boolean pageBreakBefore) {
        Element paragraph = document.createElementNS(WORD_NS, "w:p");
        paragraph.appendChild(createNoIndentParagraphProperties(document));
        Element run = document.createElementNS(WORD_NS, "w:r");

        if (pageBreakBefore) {
            Element pageBreak = document.createElementNS(WORD_NS, "w:br");
            pageBreak.setAttributeNS(WORD_NS, "w:type", "page");
            run.appendChild(pageBreak);
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

    private Element createNoIndentParagraphProperties(Document document) {
        Element paragraphProperties = document.createElementNS(WORD_NS, "w:pPr");
        Element indentation = document.createElementNS(WORD_NS, "w:ind");
        indentation.setAttributeNS(WORD_NS, "w:left", "0");
        indentation.setAttributeNS(WORD_NS, "w:right", "0");
        indentation.setAttributeNS(WORD_NS, "w:firstLine", "0");
        indentation.setAttributeNS(WORD_NS, "w:hanging", "0");
        paragraphProperties.appendChild(indentation);
        return paragraphProperties;
    }

    private Element findDocumentBody(Document document) {
        NodeList nodeList = document.getElementsByTagNameNS(WORD_NS, "body");
        if (nodeList.getLength() == 0 || !(nodeList.item(0) instanceof Element body)) {
            throw new IllegalStateException("Cannot find word document body");
        }
        return body;
    }

    private void appendParagraphToBody(Element body, Element paragraph) {
        Node sectionProperties = findDirectChild(body, "sectPr");
        if (sectionProperties != null) {
            body.insertBefore(paragraph, sectionProperties);
            return;
        }
        body.appendChild(paragraph);
    }

    /**
      * 说明：该方法注释已修复（原注释出现乱码）。
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
      * 说明：该方法注释已修复（原注释出现乱码）。
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
      * 说明：该方法注释已修复（原注释出现乱码）。
     */
    private Document readXml(Path xmlPath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try (InputStream inputStream = Files.newInputStream(xmlPath)) {
            return factory.newDocumentBuilder().parse(inputStream);
        }
    }

    /**
      * 说明：该方法注释已修复（原注释出现乱码）。
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
      * 说明：该方法注释已修复（原注释出现乱码）。
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

    private Path resolveSkillPath() {
        String configured = firstNonBlank(
                properties.getSkillPath(),
                System.getProperty("docx.agent.skill-path"),
                System.getenv("DOCX_AGENT_SKILL_PATH")
        );
        if (isBlank(configured)) {
            configured = "C:/Users/szh/.codex/skills/docx";
        }
        Path path = Path.of(configured).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            throw new IllegalStateException("Docx skill path does not exist: " + path);
        }
        return path;
    }

    private Path resolveWorkingDirectory(Path skillPath) {
        String configured = firstNonBlank(
                properties.getShellWorkingDirectory(),
                System.getProperty("docx.agent.shell-working-directory"),
                System.getenv("DOCX_AGENT_SHELL_WORKING_DIRECTORY")
        );
        if (isBlank(configured)) {
            return skillPath.toAbsolutePath().normalize();
        }
        Path path = Path.of(configured);
        if (path.isAbsolute()) {
            return path.toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.dir"), configured).toAbsolutePath().normalize();
    }

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
            throw new IllegalStateException("Python dependencies for docx-agent are missing: " + dependencyResult.output());
        }
    }

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
            return new ProcessResult(exitCode, output);
        } catch (Exception e) {
            return new ProcessResult(-1, e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }
    /**
      * 说明：该方法注释已修复（原注释出现乱码）。
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
      * 说明：该方法注释已修复（原注释出现乱码）。
     */
    private OpenAiChatModel buildOpenAiChatModel() {
        LlmEndpointResolver.ResolvedEndpoint endpoint = llmEndpointResolver.resolveDocxAgent();
        if (endpoint == null || !endpoint.isConfigured()) {
            throw new IllegalStateException("Missing LLM endpoint for docx-agent");
        }
        return OpenAiChatModel.builder()
                .baseUrl(endpoint.getBaseUrl())
                .apiKey(endpoint.getApiKey())
                .modelName(endpoint.getModel())
                .temperature(properties.getTemperature())
                .maxCompletionTokens(properties.getMaxCompletionTokens())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }
    /**
      * 说明：该方法注释已修复（原注释出现乱码）。
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
      * 说明：该方法注释已修复（原注释出现乱码）。
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

    private String normalizeMatchingText(String text) {
        return normalizeText(text)
                .replaceAll("[\\s\\p{Punct}\\uFF0C\\u3002\\uFF1B\\uFF1A\\uFF08\\uFF09]", "")
                .toLowerCase(Locale.ROOT);
    }

    private String describeOperation(PatchOperation operation) {
        String type = operation == null ? "unknown" : firstNonBlank(operation.type, "unknown");
        String anchor = operation == null ? null : firstNonBlank(operation.anchor, operation.find);
        return "type=" + type + ", anchor=" + (isBlank(anchor) ? "<empty>" : anchor);
    }

    private String buildDocumentSummaryLine(PatchOperation operation) {
        String type = operation == null ? "unknown" : firstNonBlank(operation.type, "unknown");
        String location = abbreviateSummaryText(firstNonBlank(
                operation == null ? null : operation.anchor,
                operation == null ? null : operation.find,
                "related paragraph"
        ));

        if ("replace_text_in_paragraph".equals(type)) {
            return "Adjusted wording near \"" + location + "\".";
        }
        if ("append_sentence_to_paragraph".equals(type)) {
            return "Added clarification near \"" + location + "\".";
        }
        if ("insert_paragraph_after".equals(type)) {
            return "Inserted a paragraph after \"" + location + "\".";
        }
        return "Modified content near \"" + location + "\".";
    }

    private String abbreviateSummaryText(String text) {
        String normalized = normalizeText(text);
        if (isBlank(normalized)) {
            return "related paragraph";
        }
        int maxLength = 24;
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String buildResultMessage(Path outputDocument, Path patchPlanFile, PatchApplyResult patchApplyResult) {
        StringBuilder message = new StringBuilder()
                .append("Document revised with docx skill pipeline. Output file: ")
                .append(outputDocument)
                .append(" . Patch plan: ")
                .append(patchPlanFile)
                .append(" . Applied operations: ")
                .append(patchApplyResult.appliedCount());
        if (patchApplyResult.appliedCount() > 0) {
            message.append(" . Modification summary appended to document tail.");
        }
        if (!patchApplyResult.skippedOperationMessages().isEmpty()) {
            message.append(" . Skipped operations: ").append(patchApplyResult.skippedOperationMessages().size());
        }
        return message.toString();
    }

    private boolean isPatchApplyAcceptable(PatchApplyResult applyResult, int minAppliedOperations) {
        if (applyResult == null || applyResult.appliedCount() < minAppliedOperations) {
            return false;
        }
        int total = applyResult.appliedCount() + applyResult.skippedOperationMessages().size();
        if (total <= 0) {
            return false;
        }
        double successRatio = (double) applyResult.appliedCount() / total;
        return successRatio >= PATCH_APPLY_MIN_SUCCESS_RATIO;
    }

    private String buildPatchRetryFailureMessage(int maxAttempts, int minAppliedOperations, PatchApplyResult lastApplyResult) {
        if (lastApplyResult == null) {
            return "Patch generation failed after " + maxAttempts + " attempts: no apply result was produced.";
        }
        int appliedCount = lastApplyResult.appliedCount();
        int skippedCount = lastApplyResult.skippedOperationMessages().size();
        int total = appliedCount + skippedCount;
        double successRatio = total <= 0 ? 0D : (double) appliedCount / total;
        return "Patch generation failed after " + maxAttempts + " attempts. "
                + "Required at least " + minAppliedOperations + " applied operations and "
                + "a success ratio of at least " + (int) (PATCH_APPLY_MIN_SUCCESS_RATIO * 100) + "%, but got "
                + appliedCount + " applied operations with success ratio "
                + String.format(Locale.ROOT, "%.1f%%", successRatio * 100) + ". "
                + "Skipped operations: " + skippedCount
                + ". Last warning: " + firstNonBlank(lastApplyResult.warningMessage(), "none");
    }

    private String readPatchPlanFileSafe(Path patchPlanFile) {
        if (patchPlanFile == null || !Files.isRegularFile(patchPlanFile)) {
            return "{}";
        }
        try {
            return Files.readString(patchPlanFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "{}";
        }
    }

    private String formatSkippedMessages(List<String> skippedMessages) {
        if (skippedMessages == null || skippedMessages.isEmpty()) {
            return "- none";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < skippedMessages.size(); i++) {
            builder.append(i + 1).append(". ").append(skippedMessages.get(i)).append("\n");
        }
        return builder.toString().trim();
    }

    private int normalizePositive(Integer value, int defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
    }

    /**
      * 说明：该方法注释已修复（原注释出现乱码）。
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
      * 说明：该方法注释已修复（原注释出现乱码）。
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }


    /**
      * 说明：该方法注释已修复（原注释出现乱码）。
     */
    private record ProcessResult(int exitCode, String output) {
    }

    public record ModifyDocumentResult(
            Path outputDocument,
            Path workDirectory,
            Path patchPlanFile,
            String message,
            int appliedOperationCount,
            int skippedOperationCount,
            List<String> skippedOperationMessages,
            String warningMessage
    ) {
    }

    private record PatchApplyResult(
            int appliedCount,
            List<String> skippedOperationMessages,
            String warningMessage
    ) {
    }

    private record PatchExecutionResult(
            PatchApplyResult patchApplyResult,
            Path finalPatchPlanFile
    ) {
    }

    /**
      * 说明：该方法注释已修复（原注释出现乱码）。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PatchPlan {
        public List<PatchOperation> operations = new ArrayList<>();
    }

    /**
      * 说明：该方法注释已修复（原注释出现乱码）。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PatchOperation {
        public String type;      // 操作类型
        public String anchor;    // 定位锚点
        public String find;      // 待查找文本
        public String replace;   // 替换后的文本
        public String append;    // 追加文本
        public String text;      // 新段落文本
    }
}






