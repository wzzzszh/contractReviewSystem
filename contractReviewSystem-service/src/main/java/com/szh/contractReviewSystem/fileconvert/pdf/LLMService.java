package com.szh.contractReviewSystem.fileconvert.pdf;

public interface LLMService {
    
    String call(String prompt) throws Exception;
}
