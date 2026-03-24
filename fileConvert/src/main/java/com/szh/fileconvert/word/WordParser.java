package com.szh.fileconvert.word;

import com.szh.fileconvert.DocumentParser;
import com.szh.fileconvert.base.MarkdownBuilder;
import com.szh.fileconvert.base.MarkdownResult;
import com.szh.fileconvert.base.ParseContext;
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

    /**
     * 灏嗕竴涓钀借浆鎹负甯︽牸寮忕殑 Markdown 鏂囨湰锛�
     * - 鍒楄〃锛氭牴鎹� numID 绠�鍗曡瘑鍒负鏃犲簭鍒楄〃 "- "
     * - 绮椾綋锛歳un.isBold() -> **text**
     * - 鏂滀綋锛歳un.isItalic() -> *text*
     * - 瓒呴摼鎺ワ細XWPFHyperlinkRun -> [text](url)
     */
    private String paragraphToMarkdown(XWPFParagraph p) {
        StringBuilder sb = new StringBuilder();

        // 绠�鍗曞垽鏂槸鍚︿负鍒楄〃椤癸紙鏇村鏉傜殑鍙互鏍规嵁 numFmt 鍐冲畾鏈夊簭/鏃犲簭锛�
        if (p.getNumID() != null) {
            sb.append("- ");
        }

        for (int i = 0; i < p.getRuns().size(); i++) {
            XWPFRun run = p.getRuns().get(i);
            String text = run.toString();
            if (text == null || text.isEmpty()) {
                continue;
            }

            // 澶勭悊瓒呴摼鎺�
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

            // 绠�鍗曞鐞嗘崲琛岋細Word 涓� run 閲岀殑鎹㈣杞垚 Markdown 鐨勪袱绌烘牸+鎹㈣
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
