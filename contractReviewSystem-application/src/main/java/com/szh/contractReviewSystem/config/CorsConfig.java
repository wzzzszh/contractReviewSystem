package com.szh.contractReviewSystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置类
 */
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 设置允许跨域请求的域名
        config.addAllowedOriginPattern("*");
        // 设置允许跨域请求的请求头
        config.addAllowedHeader("*");
        // 设置允许跨域请求的请求方法
        config.addAllowedMethod("*");
        // 设置是否允许携带凭证
        config.setAllowCredentials(true);
        // 设置预检请求的有效期，单位为秒
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}