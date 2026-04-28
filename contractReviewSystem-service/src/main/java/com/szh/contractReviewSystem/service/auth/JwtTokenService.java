package com.szh.contractReviewSystem.service.auth;

import com.szh.contractReviewSystem.config.AuthJwtProperties;
import com.szh.contractReviewSystem.entity.user.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtTokenService {

    private static final String TOKEN_TYPE = "tokenType";
    private static final String ACCESS_TOKEN = "access";
    private static final String REFRESH_TOKEN = "refresh";

    private final AuthJwtProperties properties;

    public JwtTokenService(AuthJwtProperties properties) {
        this.properties = properties;
    }

    public String createAccessToken(UserEntity user) {
        return createToken(user, ACCESS_TOKEN, accessTokenTtl());
    }

    public String createRefreshToken(UserEntity user) {
        return createToken(user, REFRESH_TOKEN, refreshTokenTtl());
    }

    public Claims parseValidToken(String token) {
        return Jwts.parser()
                .setSigningKey(properties.getSecret())
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isAccessToken(Claims claims) {
        return hasType(claims, ACCESS_TOKEN);
    }

    public boolean isRefreshToken(Claims claims) {
        return hasType(claims, REFRESH_TOKEN);
    }

    public Long resolveUserId(Claims claims) {
        Object rawUserId = claims == null ? null : claims.get("userId");
        if (rawUserId instanceof Number number) {
            return number.longValue();
        }
        if (rawUserId instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public long accessTokenExpiresInSeconds() {
        return accessTokenTtl().getSeconds();
    }

    private String createToken(UserEntity user, String tokenType, Duration ttl) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + ttl.toMillis());
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put(TOKEN_TYPE, tokenType);

        return Jwts.builder()
                .setClaims(claims)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setExpiration(expiresAt)
                .signWith(SignatureAlgorithm.HS512, properties.getSecret())
                .compact();
    }

    private boolean hasType(Claims claims, String expectedType) {
        return claims != null && expectedType.equals(claims.get(TOKEN_TYPE, String.class));
    }

    private Duration accessTokenTtl() {
        return Duration.ofMinutes(properties.getAccessExpirationMinutes());
    }

    private Duration refreshTokenTtl() {
        return Duration.ofDays(properties.getRefreshExpirationDays());
    }
}
