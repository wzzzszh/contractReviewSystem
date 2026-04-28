package com.szh.contractReviewSystem.controller;

import com.szh.contractReviewSystem.annotation.RequiresPermissions;
import com.szh.contractReviewSystem.common.Result;
import com.szh.contractReviewSystem.controller.notdb.BaseController;
import com.szh.contractReviewSystem.entity.user.UserEntity;
import com.szh.contractReviewSystem.model.request.CreateUserRequest;
import com.szh.contractReviewSystem.service.db.UserDataService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/api/db/users")
public class UserDataController extends BaseController {

    private final UserDataService userDataService;

    public UserDataController(UserDataService userDataService) {
        this.userDataService = userDataService;
    }

    @RequiresPermissions("user:create")
    @PostMapping
    public Result<UserEntity> createUser(@Valid @RequestBody CreateUserRequest request) {
        return success("用户创建成功", userDataService.createUser(request));
    }

    @RequiresPermissions("user:view")
    @GetMapping("/{id}")
    public Result<UserEntity> getUser(@PathVariable @NotNull(message = "用户ID不能为空") Long id) {
        return success(userDataService.getById(id));
    }

    @RequiresPermissions("user:list")
    @GetMapping
    public Result<List<UserEntity>> listUsers() {
        return success(userDataService.listUsers());
    }
}
