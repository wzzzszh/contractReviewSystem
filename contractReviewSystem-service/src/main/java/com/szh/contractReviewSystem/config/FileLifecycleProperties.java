package com.szh.contractReviewSystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "file.lifecycle")
public class FileLifecycleProperties {

    private boolean cleanupEnabled = true;

    private long tempTtlHours = 24;

    private String docxAgentWorkRoot = "work/docx-agent";
}
