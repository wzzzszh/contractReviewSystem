package com.szh.contractReviewSystem.fileconvert.base;

import java.util.List;

public class MarkdownResult {

    private final String markdown;
    private final List<Section> sections;

    public MarkdownResult(String markdown, List<Section> sections) {
        this.markdown = markdown;
        this.sections = sections;
    }

    public String getMarkdown() {
        return markdown;
    }

    public List<Section> getSections() {
        return sections;
    }
}
