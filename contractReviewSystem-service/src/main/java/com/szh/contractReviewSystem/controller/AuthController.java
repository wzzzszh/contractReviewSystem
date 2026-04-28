package com.szh.contractReviewSystem.controller;

import com.szh.contractReviewSystem.common.Result;
import com.szh.contractReviewSystem.controller.notdb.BaseController;
import com.szh.contractReviewSystem.entity.user.UserEntity;
import com.szh.contractReviewSystem.model.request.CreateUserRequest;
import com.szh.contractReviewSystem.model.request.LoginRequest;
import com.szh.contractReviewSystem.model.request.RefreshTokenRequest;
import com.szh.contractReviewSystem.model.response.LoginResponse;
import com.szh.contractReviewSystem.model.response.TokenPair;
import com.szh.contractReviewSystem.service.auth.AuthService;
import com.szh.contractReviewSystem.service.db.UserDataService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController extends BaseController {

    private final AuthService authService;

    private final UserDataService userDataService;

    public AuthController(AuthService authService, UserDataService userDataService) {
        this.authService = authService;
        this.userDataService = userDataService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return success("login success", authService.login(request));
    }

    @PostMapping("/register")
    public Result<UserEntity> register(@Valid @RequestBody CreateUserRequest request) {
        return success("register success", userDataService.createUser(request));
    }

    @PostMapping("/refresh")
    public Result<TokenPair> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return success(authService.refresh(request.getRefreshToken()));
    }
}
