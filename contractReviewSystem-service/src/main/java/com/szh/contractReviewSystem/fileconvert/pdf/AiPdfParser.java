package com.szh.contractReviewSystem.fileconvert.pdf;

import com.szh.contractReviewSystem.fileconvert.DocumentParser;
import com.szh.contractReviewSystem.fileconvert.base.MarkdownResult;
import com.szh.contractReviewSystem.fileconvert.base.ParseContext;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.util.List;

public class AiPdfParser implements DocumentParser {
    
    private final LLMService llmService;
    private boolean enableParallel = true;
    private int parallelThreads = 4;
    
    public AiPdfParser(LLMService llmService) {
        this.llmService = llmService;
    }
    
    public AiPdfParser setEnableParallel(boolean enableParallel) {
        this.enableParallel = enableParallel;
        return this;
    }
    public AiPdfParser setParallelThreads(int parallelThreads) {
        this.parallelThreads = parallelThreads;
        return this;
    }
    @Override
    public boolean supports(String fileType) {
        return "pdf".equals(fileType);
    }
    
    @Override
    public MarkdownResult parse(File file, ParseContext context) throws Exception {
        long startTime = System.currentTimeMillis();
        String rawText = extractRawText(file);
        System.out.println("[1] PDF文本提取完成，耗时: " + (System.currentTimeMillis() - startTime) + "ms");
        
        PdfTextPreprocessor.PreprocessResult preprocessResult = PdfTextPreprocessor.preprocess(rawText);
        System.out.println("[2] 文本预处理完成，耗时: " + (System.currentTimeMillis() - startTime) + "ms");
        System.out.println("    预处理后文本长度: " + preprocessResult.cleanedText.length() + " 字符");
        
        String finalMarkdown;
        
        if (enableParallel && preprocessResult.cleanedText.length() > 1500) {
            finalMarkdown = processParallel(preprocessResult, context, startTime);
        } else {
            finalMarkdown = processSingle(preprocessResult.cleanedText, context, startTime);
        }
        
        return new MarkdownResult(finalMarkdown, null);
    }
    
    private String processParallel(PdfTextPreprocessor.PreprocessResult preprocessResult, 
                                   ParseContext context, long startTime) {
        List<TextChunker.TextChunk> chunks = TextChunker.chunkByClause(preprocessResult.blocks);
        System.out.println("[3] 文本分块完成，共 " + chunks.size() + " 个块，耗时: " + 
                          (System.currentTimeMillis() - startTime) + "ms");
        
        ParallelLLMProcessor processor = new ParallelLLMProcessor(llmService, parallelThreads);
        
        long processStart = System.currentTimeMillis();
        List<ParallelLLMProcessor.ChunkResult> results = processor.processChunks(chunks, context);
        System.out.println("[4] 并行LLM处理完成，耗时: " + (System.currentTimeMillis() - processStart) + "ms");
        
        String merged = processor.mergeResults(results);
        processor.shutdown();
        
        System.out.println("[5] 结果合并完成，总耗时: " + (System.currentTimeMillis() - startTime) + "ms");
        
        return merged;
    }
    
    private String processSingle(String cleanedText, ParseContext context, long startTime) {
        System.out.println("[3] 使用单次LLM处理...");
        
        long processStart = System.currentTimeMillis();
        String prompt = buildPrompt(cleanedText, context);
        
        try {
            String result = llmService.call(prompt);
            System.out.println("[4] LLM处理完成，耗时: " + (System.currentTimeMillis() - processStart) + "ms");
            return result;
        } catch (Exception e) {
            throw new RuntimeException("LLM处理失败: " + e.getMessage(), e);
        }
    }
    
    private String extractRawText(File file) throws Exception {
        PDDocument document = PDDocument.load(file);
        
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setAddMoreFormatting(true);
        stripper.setLineSeparator("\n");
        stripper.setParagraphStart("\n\n");
        stripper.setParagraphEnd("");
        stripper.setPageStart("\n\n---PAGE---\n\n");
        stripper.setPageEnd("");
        
        String text = stripper.getText(document);
        System.out.println("PDF文本提取完成，文本: " + text);
        document.close();
        return text;
    }
    
    private String buildPrompt(String text, ParseContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请将以下文本转换为结构化的Markdown格式。\n");
        prompt.append("要求：\n");
        prompt.append("1. 识别标题层级，使用 # ## ### 等标记\n");
        prompt.append("2. 保留列表格式（有序和无序）\n");
        prompt.append("3. 直接输出Markdown，不要解释\n");
        
        if (context.isContractMode()) {
            prompt.append("4. 这是合同文档，识别条款结构（第X条格式）\n");
        }
        
        prompt.append("\n文本内容：\n");
        prompt.append(text);
        
        return prompt.toString();
    }
}
