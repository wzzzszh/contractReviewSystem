package com.szh.contractReviewSystem.fileconvert.pdf;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class ArkConfigLoader {
    
    private static final String CONFIG_FILE = "application-test.yml";
    private static Map<String, Object> config;
    
    static {
        loadConfig();
    }
    
    @SuppressWarnings("unchecked")
    private static void loadConfig() {
        Yaml yaml = new Yaml();
        try (InputStream is = ArkConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                Map<String, Object> data = yaml.load(is);
                config = (Map<String, Object>) data.get("ark");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config: " + e.getMessage(), e);
        }
    }
    
    public static String getApiKey() {
        if (config == null) {
            throw new RuntimeException("Config not loaded");
        }
        String apiKey = (String) config.get("api-key");
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("${")) {
            String envKey = System.getenv("ARK_API_KEY");
            if (envKey != null && !envKey.isEmpty()) {
                return envKey;
            }
            throw new RuntimeException("API Key not configured. Please set ARK_API_KEY environment variable or configure api-key in application-test.yml");
        }
        return apiKey;
    }
    
    public static String getBaseUrl() {
        if (config == null) {
            return "https://ark.cn-beijing.volces.com/api/v3";
        }
        return (String) config.getOrDefault("base-url", "https://ark.cn-beijing.volces.com/api/v3");
    }
    
    public static String getModel() {
        if (config == null) {
            throw new RuntimeException("Config not loaded");
        }
        return (String) config.get("model");
    }
}
