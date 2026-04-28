package com.szh.contractReviewSystem.model.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken cannot be blank")
    private String refreshToken;
}
