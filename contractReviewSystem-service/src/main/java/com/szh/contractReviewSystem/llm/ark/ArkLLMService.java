package com.szh.contractReviewSystem.llm.ark;

import com.szh.contractReviewSystem.llm.LLMService;

import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ArkLLMService implements LLMService {
    
    private final ArkService arkService;
    private final String model;
    
    public ArkLLMService(String apiKey, String model) {
        ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
        Dispatcher dispatcher = new Dispatcher();
        this.arkService = ArkService.builder()
                .dispatcher(dispatcher)
                .connectionPool(connectionPool)
                .baseUrl("https://ark.cn-beijing.volces.com/api/v3")
                .apiKey(apiKey)
                .build();
        this.model = model;
    }
    @Override
    public String call(String prompt) throws Exception {
        final List<ChatMessage> messages = new ArrayList<>();
        final ChatMessage userMessage = ChatMessage.builder()
                .role(ChatMessageRole.USER)
                .content(prompt)
                .build();
        messages.add(userMessage);
        
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .build();
        
        StringBuilder result = new StringBuilder();
        arkService.createChatCompletion(chatCompletionRequest)
                .getChoices()
                .forEach(choice -> result.append(choice.getMessage().getContent()));
        
        return result.toString();
    }
    
    public void shutdown() {
        arkService.shutdownExecutor();
    }
}
