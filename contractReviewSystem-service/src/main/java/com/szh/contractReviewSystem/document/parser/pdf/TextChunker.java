package com.szh.contractReviewSystem.document.parser.pdf;

import java.util.ArrayList;
import java.util.List;

public class TextChunker {
    
    private static final int DEFAULT_CHUNK_SIZE = 1500;
    private static final int OVERLAP_SIZE = 100;
    
    public static List<TextChunk> chunkByClause(List<PdfTextPreprocessor.TextBlock> blocks) {
        List<TextChunk> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentSize = 0;
        int chunkIndex = 0;
        
        for (PdfTextPreprocessor.TextBlock block : blocks) {
            int blockSize = block.content.length();
            
            if ("clause".equals(block.type) && currentSize > 0) {
                chunks.add(new TextChunk(currentChunk.toString().trim(), chunkIndex++));
                currentChunk = new StringBuilder();
                currentSize = 0;
            }
            
            currentChunk.append(block.content).append("\n\n");
            currentSize += blockSize;
            
            if (currentSize >= DEFAULT_CHUNK_SIZE) {
                chunks.add(new TextChunk(currentChunk.toString().trim(), chunkIndex++));
                currentChunk = new StringBuilder();
                currentSize = 0;
            }
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(new TextChunk(currentChunk.toString().trim(), chunkIndex));
        }
        
        return chunks;
    }
    
    public static List<TextChunk> chunkWithOverlap(String text, int chunkSize) {
        List<TextChunk> chunks = new ArrayList<>();
        
        if (text.length() <= chunkSize) {
            chunks.add(new TextChunk(text, 0));
            return chunks;
        }
        
        int start = 0;
        int chunkIndex = 0;
        
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            
            if (end < text.length()) {
                int lastNewline = text.lastIndexOf('\n', end);
                if (lastNewline > start + chunkSize / 2) {
                    end = lastNewline;
                }
            }
            
            String chunkText = text.substring(start, end).trim();
            if (!chunkText.isEmpty()) {
                chunks.add(new TextChunk(chunkText, chunkIndex++));
            }
            
            start = end - OVERLAP_SIZE;
            if (start <= chunks.get(chunks.size() - 1).text.length() / 2) {
                start = end;
            }
        }
        
        return chunks;
    }
    
    public static class TextChunk {
        public String text;
        public int index;
        
        public TextChunk(String text, int index) {
            this.text = text;
            this.index = index;
        }
        
        @Override
        public String toString() {
            return "Chunk[" + index + "]: " + text.substring(0, Math.min(50, text.length())) + "...";
        }
    }
}
