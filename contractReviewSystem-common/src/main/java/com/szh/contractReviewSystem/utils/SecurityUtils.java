package com.szh.contractReviewSystem.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 安全工具类
 */
public class SecurityUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);
    
    /**
     * MD5加密
     */
    public static String md5(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(str.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("MD5加密失败", e);
            return null;
        }
    }
    
    /**
     * SHA256加密
     */
    public static String sha256(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(str.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA256加密失败", e);
            return null;
        }
    }
    
    /**
     * Base64编码
     */
    public static String base64Encode(String str) {
        if (str == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(str.getBytes());
    }
    
    /**
     * Base64解码
     */
    public static String base64Decode(String str) {
        if (str == null) {
            return null;
        }
        try {
            return new String(Base64.getDecoder().decode(str));
        } catch (Exception e) {
            logger.error("Base64解码失败", e);
            return null;
        }
    }
    
    /**
     * 生成随机盐值
     */
    public static String generateSalt() {
        return StringUtils.randomString(16);
    }
    
    /**
     * 加盐MD5加密
     */
    public static String saltMd5(String password, String salt) {
        if (password == null || salt == null) {
            return null;
        }
        return md5(password + salt);
    }
}