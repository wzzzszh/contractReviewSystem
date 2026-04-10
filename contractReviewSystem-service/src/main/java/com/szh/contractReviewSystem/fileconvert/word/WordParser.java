package com.szh.contractReviewSystem.fileconvert.word;

import com.szh.contractReviewSystem.fileconvert.DocumentParser;
import com.szh.contractReviewSystem.fileconvert.base.MarkdownBuilder;
import com.szh.contractReviewSystem.fileconvert.base.MarkdownResult;
import com.szh.contractReviewSystem.fileconvert.base.ParseContext;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.File;
import java.io.FileInputStream;

public class WordParser implements DocumentParser {

    @Override
    public boolean supports(String fileType) {
        return "docx".equals(fileType);
    }

    @Override
    public MarkdownResult parse(File file, ParseContext context) throws Exception {

        XWPFDocument doc = new XWPFDocument(new FileInputStream(file));
        MarkdownBuilder builder = new MarkdownBuilder();

        for (XWPFParagraph p : doc.getParagraphs()) {
            String text = p.getText();
            if (text == null || text.trim().isEmpty()) {
                continue;
            }

            if (isHeading(p)) {
                int level = extractHeadingLevel(p);
                builder.addHeading(text.trim(), level);
            } else {
                String mdParagraph = paragraphToMarkdown(p).trim();
                if (!mdParagraph.isEmpty()) {
                    builder.addParagraph(mdParagraph);
                }
            }
        }

        return builder.build();
    }

    private String paragraphToMarkdown(XWPFParagraph p) {
        StringBuilder sb = new StringBuilder();

        if (p.getNumID() != null) {
            sb.append("- ");
        }

        for (int i = 0; i < p.getRuns().size(); i++) {
            XWPFRun run = p.getRuns().get(i);
            String text = run.toString();
            if (text == null || text.isEmpty()) {
                continue;
            }

            if (run instanceof XWPFHyperlinkRun) {
                XWPFHyperlinkRun hyperlinkRun = (XWPFHyperlinkRun) run;
                String url = null;
                if (hyperlinkRun.getHyperlink(p.getDocument()) != null) {
                    url = hyperlinkRun.getHyperlink(p.getDocument()).getURL();
                }
                if (url != null && !url.isEmpty()) {
                    sb.append("[").append(text).append("](").append(url).append(")");
                    continue;
                }
            }

            boolean bold = run.isBold();
            boolean italic = run.isItalic();

            if (bold) {
                sb.append("**");
            }
            if (italic) {
                sb.append("*");
            }

            sb.append(text.replace("\n", "  \n"));

            if (italic) {
                sb.append("*");
            }
            if (bold) {
                sb.append("**");
            }
        }

        return sb.toString();
    }

    private boolean isHeading(XWPFParagraph p) {
        return p.getStyle() != null && p.getStyle().startsWith("Heading");
    }

    private int extractHeadingLevel(XWPFParagraph p) {
        return Integer.parseInt(p.getStyle().replace("Heading", ""));
    }
}
