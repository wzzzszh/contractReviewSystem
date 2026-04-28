package com.szh.contractReviewSystem.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.szh.contractReviewSystem.common.Result;
import com.szh.contractReviewSystem.exception.BusinessExceptionEnum;
import com.szh.contractReviewSystem.service.auth.JwtTokenService;
import com.szh.contractReviewSystem.utils.UserContextHolder;
import io.jsonwebtoken.Claims;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final ObjectMapper objectMapper;

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationInterceptor(ObjectMapper objectMapper, JwtTokenService jwtTokenService) {
        this.objectMapper = objectMapper;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        try {
            String token = resolveToken(request);
            Claims claims = token == null ? null : jwtTokenService.parseValidToken(token);
            if (claims == null || !jwtTokenService.isAccessToken(claims)) {
                writeUnauthorized(response, BusinessExceptionEnum.UNAUTHORIZED.getMessage());
                return false;
            }

            Long userId = jwtTokenService.resolveUserId(claims);
            if (userId == null || userId <= 0) {
                writeUnauthorized(response, BusinessExceptionEnum.UNAUTHORIZED.getMessage());
                return false;
            }

            // Downstream services can read the authenticated user id from this request context.
            UserContextHolder.setUserId(userId);
            return true;
        } catch (Exception e) {
            writeUnauthorized(response, BusinessExceptionEnum.UNAUTHORIZED.getMessage());
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || authorization.trim().isEmpty()) {
            return null;
        }
        String normalized = authorization.trim();
        if (normalized.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return normalized.substring(BEARER_PREFIX.length()).trim();
        }
        return normalized;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Result<?> result = Result.error(BusinessExceptionEnum.UNAUTHORIZED.getCode(), message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
