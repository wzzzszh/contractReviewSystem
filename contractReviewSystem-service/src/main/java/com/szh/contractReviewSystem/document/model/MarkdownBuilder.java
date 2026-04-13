package com.szh.contractReviewSystem.document.model;

import java.util.ArrayList;
import java.util.List;

public class MarkdownBuilder {

    private final StringBuilder md = new StringBuilder();
    private final List<Section> sections = new ArrayList<>();
    private Section current;

    public void addHeading(String title, int level) {
        md.append("#".repeat(level)).append(" ").append(title).append("\n\n");

        current = new Section();
        current.setTitle(title);
        current.setLevel(level);
        current.setContent("");
        sections.add(current);
    }

    public void addParagraph(String text) {
        md.append(text).append("\n\n");
        if (current != null) {
            current.setContent(current.getContent() + text + "\n");
        }
    }

    public MarkdownResult build() {
        return new MarkdownResult(md.toString(), sections);
    }
}
