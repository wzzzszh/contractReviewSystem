package com.szh.contractReviewSystem.ai;

import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AiService {
    
    private final ArkService arkService;
    private final String model;
    private final List<ChatMessage> conversationHistory;
    private String systemPrompt = "";
    
    public AiService(String apiKey, String model) {
        ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
        Dispatcher dispatcher = new Dispatcher();
        this.arkService = ArkService.builder()
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .baseUrl("https://ark.cn-beijing.volces.com/api/v3")
                .apiKey(apiKey)
                .build();
        this.model = model;
        this.conversationHistory = new ArrayList<>();
    }
    
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
    
    public String chat(String userMessage) throws Exception {
        List<ChatMessage> messages = new ArrayList<>();
        
        if (!systemPrompt.isEmpty()) {
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content(systemPrompt)
                    .build());
        }
        
        messages.add(ChatMessage.builder()
                .role(ChatMessageRole.USER)
                .content(userMessage)
                .build());
        
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .build();
        
        StringBuilder result = new StringBuilder();
        arkService.createChatCompletion(request)
                .getChoices()
                .forEach(choice -> result.append(choice.getMessage().getContent()));
        
        return result.toString();
    }
    
    public String chatWithHistory(String userMessage) throws Exception {
        if (!systemPrompt.isEmpty() && conversationHistory.isEmpty()) {
            conversationHistory.add(ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content(systemPrompt)
                    .build());
        }
        
        conversationHistory.add(ChatMessage.builder()
                .role(ChatMessageRole.USER)
                .content(userMessage)
                .build());
        
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(conversationHistory)
                .build();
        
        StringBuilder result = new StringBuilder();
        arkService.createChatCompletion(request)
                .getChoices()
                .forEach(choice -> result.append(choice.getMessage().getContent()));
        
        conversationHistory.add(ChatMessage.builder()
                .role(ChatMessageRole.ASSISTANT)
                .content(result.toString())
                .build());
        
        return result.toString();
    }
    
    public void clearHistory() {
        conversationHistory.clear();
    }
    
    public void shutdown() {
        arkService.shutdownExecutor();
    }
    
    public static void main(String[] args) throws Exception {
        String apiKey = "fdd9c300-621c-42e8-a6c6-c946bb956006";
        String model = "ep-20260331203014-58mf4";
        
        AiService aiService = new AiService(apiKey, model);
        
        System.out.println("========== 简单问答测试 ==========");
        String response1 = aiService.chat("你好，请介绍一下你自己");
        System.out.println("AI: " + response1);
        
        System.out.println("\n========== 带系统提示词测试 ==========");
        aiService.setSystemPrompt("你是一个专业的合同审查律师，善于发现合同中的风险点并给出修改建议");
        String response2 = aiService.chat("请审查以下合同条款：第一条，租赁期限为一年，租金为每月2000元");
        System.out.println("AI: " + response2);
        
        System.out.println("\n========== 多轮对话测试 ==========");
        AiService aiService2 = new AiService(apiKey, model);
        aiService2.setSystemPrompt("你是一个乐于助人的助手");
        
        String r1 = aiService2.chatWithHistory("我叫小明");
        System.out.println("AI: " + r1);
        
        String r2 = aiService2.chatWithHistory("你还记得我叫什么吗？");
        System.out.println("AI: " + r2);
        
        aiService.shutdown();
        aiService2.shutdown();
        
        System.out.println("\n测试完成！");
    }
}
