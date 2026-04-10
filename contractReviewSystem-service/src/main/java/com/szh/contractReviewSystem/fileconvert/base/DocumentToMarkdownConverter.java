package com.szh.contractReviewSystem.fileconvert.base;

import com.szh.contractReviewSystem.fileconvert.DocumentParser;
import com.szh.contractReviewSystem.fileconvert.pdf.PdfParser;
import com.szh.contractReviewSystem.fileconvert.word.WordParser;

import java.io.File;
import java.util.List;

public class DocumentToMarkdownConverter {

    private final List<DocumentParser> parsers;

    public DocumentToMarkdownConverter() {
        this.parsers = List.of(
                new WordParser(),
                new PdfParser()
        );
    }

    public MarkdownResult convert(File file, ParseContext context) throws Exception {
        String ext = getExtension(file.getName());

        for (DocumentParser parser : parsers) {
            if (parser.supports(ext)) {
                return parser.parse(file, context);
            }
        }
        throw new UnsupportedOperationException("Unsupported file type: " + ext);
    }

    private String getExtension(String name) {
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
    }
}
