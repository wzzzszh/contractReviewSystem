package com.szh.fileconvert.pdf;

import com.volcengine.ark.runtime.model.responses.constant.ResponsesConstants;
import com.volcengine.ark.runtime.model.responses.content.InputContentItemText;
import com.volcengine.ark.runtime.model.responses.item.ItemEasyMessage;
import com.volcengine.ark.runtime.model.responses.item.MessageContent;
import com.volcengine.ark.runtime.model.responses.request.CreateResponsesRequest;
import com.volcengine.ark.runtime.model.responses.request.ResponsesInput;
import com.volcengine.ark.runtime.model.responses.response.ResponseObject;
import com.volcengine.ark.runtime.service.ArkService;

/**
 * 火山引擎Ark LLM服务实现
 * 使用火山引擎豆包大模型API
 */
public class ArkLLMService implements LLMService {
    
    private final ArkService arkService;
    private final String model;
    
    public ArkLLMService(String apiKey, String model) {
        this.arkService = ArkService.builder()
                .apiKey(apiKey)
                .baseUrl("https://ark.cn-beijing.volces.com/api/v3")
                .build();
        this.model = model;
    }
    
    @Override
    public String call(String prompt) throws Exception {
        // 构建请求
        CreateResponsesRequest request = CreateResponsesRequest.builder()
                .model(model)
                .input(ResponsesInput.builder().addListItem(
                        ItemEasyMessage.builder()
                                .role(ResponsesConstants.MESSAGE_ROLE_USER)
                                .content(
                                        MessageContent.builder()
                                                .addListItem(InputContentItemText.builder().text(prompt).build())
                                                .build()
                                )
                                .build()
                ).build())
                .build();
        
        // 调用API
        ResponseObject resp = arkService.createResponse(request);
        
        // 解析响应，提取AI生成的内容
        String result = parseResponse(resp);
        
        return result;
    }
    
    /**
     * 解析响应对象，提取AI生成的文本内容
     */
    private String parseResponse(ResponseObject resp) {
        // 从响应中提取输出内容
        if (resp == null || resp.getOutput() == null || resp.getOutput().isEmpty()) {
            throw new RuntimeException("AI响应为空");
        }
        
        // 遍历output列表，找到Message类型的内容
        for (var item : resp.getOutput()) {
            if (item instanceof com.volcengine.ark.runtime.model.responses.item.ItemOutputMessage) {
                var message = (com.volcengine.ark.runtime.model.responses.item.ItemOutputMessage) item;
                if (message.getContent() != null && !message.getContent().isEmpty()) {
                    // 提取第一个文本内容
                    for (var contentItem : message.getContent()) {
                        if (contentItem instanceof com.volcengine.ark.runtime.model.responses.content.OutputContentItemText) {
                            return ((com.volcengine.ark.runtime.model.responses.content.OutputContentItemText) contentItem).getText();
                        }
                    }
                }
            }
        }
        
        throw new RuntimeException("无法从响应中提取文本内容");
    }
}