package com.szh.contractReviewSystem.document.parser.pdf;

import com.szh.contractReviewSystem.document.DocumentParser;
import com.szh.contractReviewSystem.document.model.MarkdownBuilder;
import com.szh.contractReviewSystem.document.model.MarkdownResult;
import com.szh.contractReviewSystem.document.model.ParseContext;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;

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
            if (!titleAdded) {
                builder.addHeading(trimmed, 1);
                titleAdded = true;
                continue;
            }

            if (context.isContractMode() && isClause(trimmed)) {
                builder.addHeading(trimmed, 2);
                continue;
            }

            builder.addParagraph(trimmed);
        }

        document.close();
        return builder.build();
    }

    private boolean isClause(String line) {
        return line.matches("^(第[一二三四五六七八九十百千万]+[条章])\\s*.*$") ||
                line.matches("^(\\d+[\\.、）])\\s*.*$") ||
                line.matches("^([（(]\\d+[）)])\\s*.*$");
    }
}
