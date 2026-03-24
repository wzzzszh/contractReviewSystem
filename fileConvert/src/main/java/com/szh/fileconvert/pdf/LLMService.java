package com.szh.fileconvert.pdf;

/**
 * LLM服务接口
 * 用于调用大语言模型API
 */
public interface LLMService {
    
    /**
     * 调用LLM API处理文本
     * @param prompt 提示词
     * @return AI处理后的结果
     * @throws Exception 调用异常
     */
    String call(String prompt) throws Exception;
}