package com.szh.contractReviewSystem.service.auth;

import com.szh.contractReviewSystem.entity.user.UserEntity;
import com.szh.contractReviewSystem.exception.BusinessExceptionEnum;
import com.szh.contractReviewSystem.exception.CustomException;
import com.szh.contractReviewSystem.mapper.user.UserMapper;
import com.szh.contractReviewSystem.model.request.LoginRequest;
import com.szh.contractReviewSystem.model.response.LoginResponse;
import com.szh.contractReviewSystem.model.response.TokenPair;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final int NORMAL_STATUS = 1;

    private final UserMapper userMapper;

    private final PasswordService passwordService;

    private final JwtTokenService jwtTokenService;

    public AuthService(UserMapper userMapper, PasswordService passwordService, JwtTokenService jwtTokenService) {
        this.userMapper = userMapper;
        this.passwordService = passwordService;
        this.jwtTokenService = jwtTokenService;
    }

    public LoginResponse login(LoginRequest request) {
        String username = normalizeText(request.getUsername());
        String password = normalizeText(request.getPassword());

        UserEntity user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new CustomException(BusinessExceptionEnum.USER_NOT_EXIST);
        }
        if (user.getStatus() == null || user.getStatus() != NORMAL_STATUS) {
            throw new CustomException(BusinessExceptionEnum.USER_ACCOUNT_LOCKED);
        }
        if (!passwordService.matches(password, user.getPassword())) {
            throw new CustomException(BusinessExceptionEnum.USER_PASSWORD_ERROR);
        }

        upgradeLegacyPassword(password, user);
        TokenPair tokenPair = createTokenPair(user);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(tokenPair.getAccessToken());
        response.setRefreshToken(tokenPair.getRefreshToken());
        response.setExpiresIn(tokenPair.getExpiresIn());
        // Keep old clients alive while the frontend migrates to accessToken.
        response.setToken(tokenPair.getAccessToken());
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        return response;
    }

    public TokenPair refresh(String refreshToken) {
        try {
            Claims claims = jwtTokenService.parseValidToken(normalizeText(refreshToken));
            if (!jwtTokenService.isRefreshToken(claims)) {
                throw unauthorized();
            }

            Long userId = jwtTokenService.resolveUserId(claims);
            UserEntity user = userId == null ? null : userMapper.selectById(userId);
            if (user == null || user.getStatus() == null || user.getStatus() != NORMAL_STATUS) {
                throw unauthorized();
            }
            return createTokenPair(user);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw unauthorized();
        }
    }

    private TokenPair createTokenPair(UserEntity user) {
        return new TokenPair(
                jwtTokenService.createAccessToken(user),
                jwtTokenService.createRefreshToken(user),
                jwtTokenService.accessTokenExpiresInSeconds()
        );
    }

    private void upgradeLegacyPassword(String rawPassword, UserEntity user) {
        if (!passwordService.isBcrypt(user.getPassword())) {
            // Existing plaintext demo accounts are upgraded after the first successful login.
            userMapper.updatePassword(user.getId(), passwordService.encode(rawPassword));
        }
    }

    private CustomException unauthorized() {
        return new CustomException(BusinessExceptionEnum.UNAUTHORIZED);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
