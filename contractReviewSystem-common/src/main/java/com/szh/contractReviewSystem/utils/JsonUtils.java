package com.szh.contractReviewSystem.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * JSON工具类
 */
public class JsonUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 对象转JSON字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("对象转JSON字符串失败", e);
            return null;
        }
    }
    
    /**
     * JSON字符串转对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            logger.error("JSON字符串转对象失败", e);
            return null;
        }
    }
    
    /**
     * JSON字符串转List
     */
    public static <T> List<T> fromJsonToList(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, 
                TypeFactory.defaultInstance().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            logger.error("JSON字符串转List失败", e);
            return null;
        }
    }
    
    /**
     * JSON字符串转Map
     */
    public static Map<String, Object> fromJsonToMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, 
                TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class));
        } catch (Exception e) {
            logger.error("JSON字符串转Map失败", e);
            return null;
        }
    }
    
    /**
     * 对象转换
     */
    public static <T> T convert(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        
        try {
            return objectMapper.convertValue(obj, clazz);
        } catch (Exception e) {
            logger.error("对象转换失败", e);
            return null;
        }
    }
}