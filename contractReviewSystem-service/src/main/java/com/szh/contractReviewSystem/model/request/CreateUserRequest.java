package com.szh.contractReviewSystem.model.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "password cannot be blank")
    private String password;

    private String nickname;
}
