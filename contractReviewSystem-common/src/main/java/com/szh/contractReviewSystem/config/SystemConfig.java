package com.szh.contractReviewSystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 系统配置类
 */
@Component
@ConfigurationProperties(prefix = "system")
public class SystemConfig {
    
    /**
     * 项目名称
     */
    private String name;
    
    /**
     * 版本
     */
    private String version;
    
    /**
     * 版权年份
     */
    private String copyrightYear;
    
    /**
     * 上传路径
     */
    private String uploadPath;
    
    /**
     * 头像上传路径
     */
    private String avatarPath;
    
    /**
     * 下载路径
     */
    private String downloadPath;
    
    /**
     * 获取地址开关
     */
    private boolean addressEnabled;
    
    /**
     * 是否演示模式
     */
    private boolean demoMode;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getCopyrightYear() {
        return copyrightYear;
    }
    
    public void setCopyrightYear(String copyrightYear) {
        this.copyrightYear = copyrightYear;
    }
    
    public String getUploadPath() {
        return uploadPath;
    }
    
    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }
    
    public String getAvatarPath() {
        return avatarPath;
    }
    
    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }
    
    public String getDownloadPath() {
        return downloadPath;
    }
    
    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
    }
    
    public boolean isAddressEnabled() {
        return addressEnabled;
    }
    
    public void setAddressEnabled(boolean addressEnabled) {
        this.addressEnabled = addressEnabled;
    }
    
    public boolean isDemoMode() {
        return demoMode;
    }
    
    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
    }
}