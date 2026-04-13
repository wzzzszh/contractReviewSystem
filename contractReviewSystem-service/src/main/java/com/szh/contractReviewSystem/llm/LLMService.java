package com.szh.contractReviewSystem.llm;

public interface LLMService {
    
    String call(String prompt) throws Exception;
}
