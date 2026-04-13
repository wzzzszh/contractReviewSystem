package com.szh.contractReviewSystem.document.parser.pdf;

import com.szh.contractReviewSystem.document.DocumentParser;
import com.szh.contractReviewSystem.document.model.MarkdownResult;
import com.szh.contractReviewSystem.document.model.ParseContext;
import com.szh.contractReviewSystem.llm.LLMService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;

public class AiPdfParser2 implements DocumentParser {
    
    private final LLMService llmService;
    
    public AiPdfParser2(LLMService llmService) {
        this.llmService = llmService;
    }
    
    @Override
    public boolean supports(String fileType) {
        return "pdf".equals(fileType);
    }
    
    @Override
    public MarkdownResult parse(File file, ParseContext context) throws Exception {
        String rawText = extractRawText(file);
        String prompt = buildPrompt(rawText, context);
        String structuredMarkdown = llmService.call(prompt);
        return new MarkdownResult(structuredMarkdown, null);
    }
    
    private String extractRawText(File file) throws Exception {
        PDDocument document = PDDocument.load(file);
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();
        return text;
    }
    
    private String buildPrompt(String rawText, ParseContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请将以下PDF提取的文本转换为结构化的Markdown格式。\n");
        prompt.append("请直接输出最终的Markdown内容，不要包含任何推理过程、思考步骤或额外说明。\n\n");
        prompt.append("转换要求：\n");
        prompt.append("1. 正确识别各级标题（H1-H6），使用 #、##、### 等标记\n");
        prompt.append("2. 保留加粗（**text**）、斜体（*text*）等格式\n");
        prompt.append("3. 重构列表（有序和无序）和表格\n");
        prompt.append("4. 清理页眉、页脚和页码等无关内容\n");
        prompt.append("5. 保持原文档结构和语义\n");
        prompt.append("6. 确保段落之间有适当的空行分隔\n");
        
        if (context.isContractMode()) {
            prompt.append("7. 这是合同文档，请特别识别条款结构（第X条、X.、(X)等类似格式）\n");
        }
        
        prompt.append("\n原始文本：\n");
        prompt.append(rawText);
        
        return prompt.toString();
    }
}
