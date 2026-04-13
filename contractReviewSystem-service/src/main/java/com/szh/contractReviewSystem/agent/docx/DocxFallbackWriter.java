package com.szh.contractReviewSystem.agent.docx;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class DocxFallbackWriter {

    public void writeFromDraft(Path inputDocument, Path outputDocument, String draftText) throws IOException {
        try (InputStream inputStream = Files.newInputStream(inputDocument);
             XWPFDocument document = new XWPFDocument(inputStream)) {

            while (document.getBodyElements().size() > 0) {
                document.removeBodyElement(0);
            }

            String normalized = normalizeDraft(draftText);
            for (String rawLine : normalized.split("\\R")) {
                writeLine(document, rawLine);
            }

            Path parent = outputDocument.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream outputStream = Files.newOutputStream(outputDocument)) {
                document.write(outputStream);
            }
        }
    }

    private void writeLine(XWPFDocument document, String rawLine) {
        String line = rawLine == null ? "" : rawLine.trim();
        XWPFParagraph paragraph = document.createParagraph();

        if (line.isEmpty()) {
            paragraph.setSpacingAfter(120);
            return;
        }

        if (line.startsWith("# ")) {
            paragraph.setAlignment(ParagraphAlignment.CENTER);
            writeRun(paragraph, stripMarkdown(line.substring(2)), true, 16);
            paragraph.setSpacingAfter(220);
            return;
        }
        if (line.startsWith("## ")) {
            paragraph.setAlignment(ParagraphAlignment.LEFT);
            writeRun(paragraph, stripMarkdown(line.substring(3)), true, 14);
            paragraph.setSpacingBefore(160);
            paragraph.setSpacingAfter(160);
            return;
        }
        if (line.startsWith("### ")) {
            paragraph.setAlignment(ParagraphAlignment.LEFT);
            writeRun(paragraph, stripMarkdown(line.substring(4)), true, 12);
            paragraph.setSpacingBefore(120);
            paragraph.setSpacingAfter(120);
            return;
        }

        paragraph.setAlignment(ParagraphAlignment.LEFT);
        writeRun(paragraph, stripMarkdown(line), false, 11);
        paragraph.setSpacingAfter(100);
    }

    private void writeRun(XWPFParagraph paragraph, String text, boolean bold, int fontSize) {
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontFamily("SimSun");
        run.setFontSize(fontSize);
    }

    private String normalizeDraft(String draftText) {
        if (draftText == null) {
            return "";
        }
        String normalized = draftText.replace("\r\n", "\n");
        int headingIndex = normalized.indexOf("# ");
        if (headingIndex >= 0) {
            return normalized.substring(headingIndex).trim();
        }
        int titleIndex = normalized.indexOf("劳动合同");
        if (titleIndex > 0) {
            return normalized.substring(titleIndex).trim();
        }
        return normalized.trim();
    }

    private String stripMarkdown(String value) {
        return value.replace("**", "").trim();
    }
}
