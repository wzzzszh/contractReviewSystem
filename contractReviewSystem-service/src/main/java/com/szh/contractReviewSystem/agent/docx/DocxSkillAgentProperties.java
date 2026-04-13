package com.szh.contractReviewSystem.agent.docx;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "docx-agent")
public class DocxSkillAgentProperties {

    private boolean enabled = true;
    private String apiKey;
    private String baseUrl = "https://ark.cn-beijing.volces.com/api/v3";
    private String model;
    private String skillPath;
    private String shellWorkingDirectory;
    private String pythonCommand = "python";
    private Double temperature = 0.1D;
    private Integer maxCompletionTokens = 4096;
    private Integer maxSequentialToolsInvocations = 12;
    private Integer maxStdOutChars = 20000;
    private Integer maxStdErrChars = 12000;
    private Long timeoutSeconds = 90L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSkillPath() {
        return skillPath;
    }

    public void setSkillPath(String skillPath) {
        this.skillPath = skillPath;
    }

    public String getShellWorkingDirectory() {
        return shellWorkingDirectory;
    }

    public void setShellWorkingDirectory(String shellWorkingDirectory) {
        this.shellWorkingDirectory = shellWorkingDirectory;
    }

    public String getPythonCommand() {
        return pythonCommand;
    }

    public void setPythonCommand(String pythonCommand) {
        this.pythonCommand = pythonCommand;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxCompletionTokens() {
        return maxCompletionTokens;
    }

    public void setMaxCompletionTokens(Integer maxCompletionTokens) {
        this.maxCompletionTokens = maxCompletionTokens;
    }

    public Integer getMaxSequentialToolsInvocations() {
        return maxSequentialToolsInvocations;
    }

    public void setMaxSequentialToolsInvocations(Integer maxSequentialToolsInvocations) {
        this.maxSequentialToolsInvocations = maxSequentialToolsInvocations;
    }

    public Integer getMaxStdOutChars() {
        return maxStdOutChars;
    }

    public void setMaxStdOutChars(Integer maxStdOutChars) {
        this.maxStdOutChars = maxStdOutChars;
    }

    public Integer getMaxStdErrChars() {
        return maxStdErrChars;
    }

    public void setMaxStdErrChars(Integer maxStdErrChars) {
        this.maxStdErrChars = maxStdErrChars;
    }

    public Long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
