package com.szh.fileconvert.base;

import lombok.Data;

/**
 * 解析上下文配置
 */
@Data
public class ParseContext {

    /**
     * 是否启用标题检测
     */
    private boolean enableTitleDetection = true;
    
    /**
     * 是否启用条款检测
     */
    private boolean enableClauseDetection = true;
    
    /**
     * 是否启用表格解析
     */
    private boolean enableTable = true;

    /**
     * 针对合同文档的解析模式
     */
    private boolean contractMode = true;
}