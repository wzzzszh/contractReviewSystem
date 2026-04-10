package com.szh.contractReviewSystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ark")
public class ArkConfig {
    
    private String apiKey;
    
    private String baseUrl = "https://ark.cn-beijing.volces.com/api/v3";
    
    private String model;
}
