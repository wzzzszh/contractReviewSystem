package com.szh.fileconvert.pdf;

import com.szh.fileconvert.DocumentParser;
import com.szh.fileconvert.base.MarkdownBuilder;
import com.szh.fileconvert.base.MarkdownResult;
import com.szh.fileconvert.base.ParseContext;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;

/**
 * PDF解析器
 */
public class PdfParser implements DocumentParser {

    @Override
    public boolean supports(String fileType) {
        return "pdf".equals(fileType);
    }

    @Override
    public MarkdownResult parse(File file, ParseContext context) throws Exception {

        PDDocument document = PDDocument.load(file);
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);

        MarkdownBuilder builder = new MarkdownBuilder();

        String[] lines = text.split("\n");

        boolean titleAdded = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // 第一行非空文本，当作文档标题（最小化误判）
            if (!titleAdded) {
                builder.addHeading(trimmed, 1);
                titleAdded = true;
                continue;
            }

            // 合同模式下的条款识别（仅合同模式才启用）
            if (context.isContractMode() && isClause(trimmed)) {
                builder.addHeading(trimmed, 2);
                continue;
            }

            // 其余全部当作正文，避免过度识别成标题
            builder.addParagraph(trimmed);
        }

        document.close();
        return builder.build();
    }

    /**
     * 合同条款的最小模式：第X条 / X. / （X）
     */
    private boolean isClause(String line) {
        return line.matches("^(第[一二三四五六七八九十百千万]+[条章])\\s*.*$") ||
                line.matches("^(\\d+[\\.、）])\\s*.*$") ||
                line.matches("^([（(]\\d+[）)])\\s*.*$");
    }
}