package com.szh.contractReviewSystem.ai;

import com.szh.contractReviewSystem.llm.LLMService;
import com.szh.contractReviewSystem.llm.LlmServiceFactory;

import java.util.ArrayList;
import java.util.List;

public class AiService {

    private final LLMService llmService;
    private final List<ConversationTurn> conversationHistory;
    private String systemPrompt = "";

    public AiService(LLMService llmService) {
        if (llmService == null) {
            throw new IllegalArgumentException("LLM service must not be null");
        }
        this.llmService = llmService;
        this.conversationHistory = new ArrayList<>();
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt == null ? "" : systemPrompt.trim();
    }

    public String chat(String userMessage) throws Exception {
        return llmService.call(systemPrompt, userMessage);
    }

    public String chatWithHistory(String userMessage) throws Exception {
        conversationHistory.add(new ConversationTurn("user", userMessage));
        String response = llmService.call(systemPrompt, buildHistoryPrompt());
        conversationHistory.add(new ConversationTurn("assistant", response));
        return response;
    }

    public void clearHistory() {
        conversationHistory.clear();
    }

    public void shutdown() {
        llmService.shutdown();
    }

    private String buildHistoryPrompt() {
        StringBuilder prompt = new StringBuilder();
        for (ConversationTurn turn : conversationHistory) {
            prompt.append(turn.role()).append(": ")
                    .append(turn.content() == null ? "" : turn.content())
                    .append("\n\n");
        }
        prompt.append("assistant:");
        return prompt.toString();
    }

    public static void main(String[] args) throws Exception {
        LLMService llmService = LlmServiceFactory.tryCreateDefault();
        if (llmService == null) {
            throw new IllegalStateException("No LLM provider configured");
        }

        AiService aiService = new AiService(llmService);
        aiService.setSystemPrompt("你是一个专业的合同审查律师，善于发现合同中的风险点并给出修改建议。");

        System.out.println("========== 简单问答测试 ==========");
        String response1 = aiService.chat("你好，请介绍一下你自己");
        System.out.println("AI: " + response1);

        System.out.println("\n========== 多轮对话测试 ==========");
        String r1 = aiService.chatWithHistory("我叫小明");
        System.out.println("AI: " + r1);

        String r2 = aiService.chatWithHistory("你还记得我叫什么吗？");
        System.out.println("AI: " + r2);

        aiService.shutdown();
        System.out.println("\n测试完成");
    }

    private record ConversationTurn(String role, String content) {
    }
}
