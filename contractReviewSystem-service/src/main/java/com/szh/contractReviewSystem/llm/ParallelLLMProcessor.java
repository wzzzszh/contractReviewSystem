package com.szh.contractReviewSystem.llm;

import com.szh.contractReviewSystem.document.model.ParseContext;
import com.szh.contractReviewSystem.document.parser.pdf.TextChunker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ParallelLLMProcessor {
    
    private final LLMService llmService;
    private final ExecutorService executorService;
    private final int maxParallel;
    
    public ParallelLLMProcessor(LLMService llmService, int maxParallel) {
        this.llmService = llmService;
        this.maxParallel = maxParallel;
        this.executorService = Executors.newFixedThreadPool(maxParallel);
    }
    
    public ParallelLLMProcessor(LLMService llmService) {
        this(llmService, 4);
    }
    
    public List<ChunkResult> processChunks(List<TextChunker.TextChunk> chunks, ParseContext context) {
        List<Future<ChunkResult>> futures = new ArrayList<>();
        
        for (TextChunker.TextChunk chunk : chunks) {
            Future<ChunkResult> future = executorService.submit(() -> processChunk(chunk, context));
            futures.add(future);
        }
        
        List<ChunkResult> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                ChunkResult result = futures.get(i).get();
                results.add(result);
            } catch (InterruptedException | ExecutionException e) {
                results.add(new ChunkResult(chunks.get(i).index, null, "Error: " + e.getMessage()));
            }
        }
        
        results.sort((a, b) -> Integer.compare(a.index, b.index));
        
        return results;
    }
    
    private ChunkResult processChunk(TextChunker.TextChunk chunk, ParseContext context) {
        try {
            String prompt = buildChunkPrompt(chunk.text, context);
            String result = llmService.call(prompt);
            return new ChunkResult(chunk.index, result, null);
        } catch (Exception e) {
            return new ChunkResult(chunk.index, null, "Error: " + e.getMessage());
        }
    }
    
    private String buildChunkPrompt(String chunkText, ParseContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请将以下文本转换为结构化的Markdown格式。\n");
        prompt.append("要求：\n");
        prompt.append("1. 识别标题层级，使用 # ## ### 等标记\n");
        prompt.append("2. 保留列表格式（有序和无序）\n");
        prompt.append("3. 直接输出Markdown，不要解释\n");
        
        if (context.isContractMode()) {
            prompt.append("4. 这是合同文档的一部分，识别条款结构\n");
        }
        
        prompt.append("\n文本内容：\n");
        prompt.append(chunkText);
        
        return prompt.toString();
    }
    
    public String mergeResults(List<ChunkResult> results) {
        StringBuilder merged = new StringBuilder();
        
        for (ChunkResult result : results) {
            if (result.error != null) {
                merged.append("\n<!-- Error in chunk ").append(result.index).append(": ").append(result.error).append(" -->\n");
            } else if (result.markdown != null) {
                merged.append(result.markdown).append("\n\n");
            }
        }
        
        return merged.toString().trim();
    }
    
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
    
    public static class ChunkResult {
        public int index;
        public String markdown;
        public String error;
        
        public ChunkResult(int index, String markdown, String error) {
            this.index = index;
            this.markdown = markdown;
            this.error = error;
        }
    }
}
