package com.szh.contractReviewSystem.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
public class JwtUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    
    /**
     * 密钥
     */
    private static final String SECRET = "mySecretKey";
    
    /**
     * 过期时间（毫秒）
     */
    private static final long EXPIRATION = 86400000; // 24小时
    
    /**
     * 生成JWT令牌
     *
     * @param claims 自定义声明
     * @return JWT令牌
     */
    public static String generateToken(Map<String, Object> claims) {
        Date expirationDate = new Date(System.currentTimeMillis() + EXPIRATION);
        
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
    }
    
    /**
     * 生成JWT令牌（仅包含用户名）
     *
     * @param username 用户名
     * @return JWT令牌
     */
    public static String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        return generateToken(claims);
    }
    
    /**
     * 解析JWT令牌
     *
     * @param token JWT令牌
     * @return 声明信息
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error("解析JWT令牌失败", e);
            return null;
        }
    }
    
    /**
     * 验证JWT令牌是否有效
     *
     * @param token JWT令牌
     * @return 是否有效
     */
    public static boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims != null && !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 从JWT令牌中获取用户名
     *
     * @param token JWT令牌
     * @return 用户名
     */
    public static String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            return (String) claims.get("username");
        }
        return null;
    }
}