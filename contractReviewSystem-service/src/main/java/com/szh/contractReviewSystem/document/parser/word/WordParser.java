package com.szh.contractReviewSystem.document.parser.word;

import com.szh.contractReviewSystem.document.DocumentParser;
import com.szh.contractReviewSystem.document.model.MarkdownBuilder;
import com.szh.contractReviewSystem.document.model.MarkdownResult;
import com.szh.contractReviewSystem.document.model.ParseContext;
import com.szh.contractReviewSystem.llm.LLMService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class WordParser implements DocumentParser {

    private final LLMService llmService;

    public WordParser() {
        this(null);
    }

    public WordParser(LLMService llmService) {
        this.llmService = llmService;
    }

    @Override
    public boolean supports(String fileType) {
        return "docx".equals(fileType);
    }

    @Override
    public MarkdownResult parse(File file, ParseContext context) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file))) {
            String rawText = extractRawText(doc);
            if (llmService != null) {
                String prompt = buildPrompt(rawText, context);
                String markdown = llmService.call(prompt);
                return new MarkdownResult(markdown, null);
            }
            return fallbackParse(doc, context);
        }
    }

    private String extractRawText(XWPFDocument doc) {
        StringBuilder text = new StringBuilder();

        for (XWPFParagraph paragraph : doc.getParagraphs()) {
            String line = normalizeText(paragraph.getText());
            if (!line.isEmpty()) {
                text.append(line).append("\n\n");
            }
        }

        for (XWPFTable table : doc.getTables()) {
            text.append("[TABLE]").append("\n");
            for (XWPFTableRow row : table.getRows()) {
                List<String> cells = new ArrayList<>();
                for (XWPFTableCell cell : row.getTableCells()) {
                    String cellText = normalizeText(cell.getText());
                    cells.add(cellText);
                }
                text.append(String.join(" | ", cells)).append("\n");
            }
            text.append("[/TABLE]").append("\n\n");
        }

        return text.toString().trim();
    }

    private String buildPrompt(String text, ParseContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请将以下从 Word 合同提取的原始文本转换为结构化 Markdown。\n");
        prompt.append("要求：\n");
        prompt.append("1. 识别合同标题、章节标题、条款标题，使用 # ## ### 分层。\n");
        prompt.append("2. 清理多余空格、空行、页眉页脚式噪音，但保留原始语义。\n");
        prompt.append("3. 列表项转成 Markdown 列表，表格尽量转成 Markdown 表格。\n");
        prompt.append("4. 直接输出 Markdown，不要解释。\n");
        if (context != null && context.isContractMode()) {
            prompt.append("5. 这是合同文档，优先识别“第X条”“一、”“一．”等条款结构。\n");
        }
        prompt.append("\n原始文本：\n");
        prompt.append(text);
        return prompt.toString();
    }

    private MarkdownResult fallbackParse(XWPFDocument doc, ParseContext context) {
        MarkdownBuilder builder = new MarkdownBuilder();
        boolean titleAdded = false;

        for (XWPFParagraph paragraph : doc.getParagraphs()) {
            String text = normalizeText(paragraphToMarkdown(paragraph));
            if (text.isEmpty()) {
                continue;
            }

            if (!titleAdded) {
                builder.addHeading(text, 1);
                titleAdded = true;
                continue;
            }

            builder.addParagraph(text);
        }

        return builder.build();
    }

    private String paragraphToMarkdown(XWPFParagraph paragraph) {
        StringBuilder sb = new StringBuilder();

        if (paragraph.getNumID() != null) {
            sb.append("- ");
        }

        for (XWPFRun run : paragraph.getRuns()) {
            String text = normalizeText(run.toString());
            if (text.isEmpty()) {
                continue;
            }

            if (run instanceof XWPFHyperlinkRun) {
                XWPFHyperlinkRun hyperlinkRun = (XWPFHyperlinkRun) run;
                String url = hyperlinkRun.getHyperlink(paragraph.getDocument()) == null
                        ? null
                        : hyperlinkRun.getHyperlink(paragraph.getDocument()).getURL();
                if (url != null && !url.isEmpty()) {
                    sb.append("[").append(text).append("](").append(url).append(")");
                    continue;
                }
            }

            if (run.isBold()) {
                sb.append("**");
            }
            if (run.isItalic()) {
                sb.append("*");
            }

            sb.append(text.replace("\n", "  \n"));

            if (run.isItalic()) {
                sb.append("*");
            }
            if (run.isBold()) {
                sb.append("**");
            }
        }

        return sb.toString();
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
}
