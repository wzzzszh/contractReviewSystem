package com.szh.contractReviewSystem.service.db;

import com.szh.contractReviewSystem.entity.user.UserEntity;
import com.szh.contractReviewSystem.exception.BusinessExceptionEnum;
import com.szh.contractReviewSystem.exception.CustomException;
import com.szh.contractReviewSystem.mapper.user.UserMapper;
import com.szh.contractReviewSystem.model.request.CreateUserRequest;
import com.szh.contractReviewSystem.service.auth.PasswordService;
import com.szh.contractReviewSystem.service.auth.PermissionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDataService {

    private final UserMapper userMapper;

    private final PasswordService passwordService;

    private final PermissionService permissionService;

    public UserDataService(UserMapper userMapper,
                           PasswordService passwordService,
                           PermissionService permissionService) {
        this.userMapper = userMapper;
        this.passwordService = passwordService;
        this.permissionService = permissionService;
    }

    public UserEntity createUser(CreateUserRequest request) {
        if (request == null) {
            throw new CustomException(BusinessExceptionEnum.PARAMETER_ERROR, "用户请求不能为空");
        }
        String username = requireText(request.getUsername(), "用户名不能为空");
        if (userMapper.selectByUsername(username) != null) {
            throw new CustomException(BusinessExceptionEnum.USER_ACCOUNT_EXIST);
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordService.encode(requireText(request.getPassword(), "password cannot be blank")));
        user.setNickname(normalizeText(request.getNickname()));
        user.setStatus(1);
        userMapper.insert(user);
        permissionService.assignDefaultRole(user.getId());
        return erasePassword(userMapper.selectById(user.getId()));
    }

    public UserEntity getById(Long id) {
        if (id == null || id <= 0) {
            throw new CustomException(BusinessExceptionEnum.PARAMETER_ERROR, "用户ID不能为空且必须大于0");
        }
        UserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new CustomException(BusinessExceptionEnum.USER_NOT_EXIST);
        }
        return erasePassword(user);
    }

    public List<UserEntity> listUsers() {
        List<UserEntity> users = new ArrayList<>(userMapper.selectAll());
        users.forEach(this::erasePassword);
        return users;
    }

    private UserEntity erasePassword(UserEntity user) {
        if (user != null) {
            // Password hashes are write-only data and should not be sent to clients.
            user.setPassword(null);
        }
        return user;
    }

    private String requireText(String value, String message) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            throw new CustomException(BusinessExceptionEnum.PARAMETER_ERROR, message);
        }
        return normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
