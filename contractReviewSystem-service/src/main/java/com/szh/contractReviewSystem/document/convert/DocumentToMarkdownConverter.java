package com.szh.contractReviewSystem.document.convert;

import com.szh.contractReviewSystem.document.DocumentParser;
import com.szh.contractReviewSystem.document.model.MarkdownResult;
import com.szh.contractReviewSystem.document.model.ParseContext;
import com.szh.contractReviewSystem.document.parser.pdf.PdfParser;
import com.szh.contractReviewSystem.document.parser.word.WordParser;
import com.szh.contractReviewSystem.llm.LLMService;
import com.szh.contractReviewSystem.llm.LlmServiceFactory;

import java.io.File;
import java.util.List;

public class DocumentToMarkdownConverter {

    private final List<DocumentParser> parsers;

    public DocumentToMarkdownConverter() {
        this(LlmServiceFactory.tryCreateDefault());
    }

    public DocumentToMarkdownConverter(LLMService llmService) {
        this.parsers = List.of(
                new WordParser(llmService),
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
