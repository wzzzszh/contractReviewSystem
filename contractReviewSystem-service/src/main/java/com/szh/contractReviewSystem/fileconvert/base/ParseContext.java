package com.szh.contractReviewSystem.fileconvert.base;

import lombok.Data;

@Data
public class ParseContext {

    private boolean enableTitleDetection = true;
    
    private boolean enableClauseDetection = true;
    
    private boolean enableTable = true;

    private boolean contractMode = true;
}
