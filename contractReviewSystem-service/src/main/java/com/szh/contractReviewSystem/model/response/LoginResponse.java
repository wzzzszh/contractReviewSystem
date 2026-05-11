package com.szh.contractReviewSystem.model.response;

import lombok.Data;

import java.util.Set;

@Data
public class LoginResponse {

    private String accessToken;

    private String refreshToken;

    private String token;

    private String tokenType = "Bearer";

    private Long expiresIn;

    private Long userId;

    private String username;

    private String nickname;

    private Set<String> permissions;
}
