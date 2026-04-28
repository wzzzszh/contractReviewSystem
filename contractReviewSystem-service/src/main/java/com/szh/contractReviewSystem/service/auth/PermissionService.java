package com.szh.contractReviewSystem.service.auth;

import com.szh.contractReviewSystem.mapper.auth.AuthPermissionMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
public class PermissionService {

    public static final String DEFAULT_USER_ROLE = "USER";

    private final AuthPermissionMapper authPermissionMapper;

    public PermissionService(AuthPermissionMapper authPermissionMapper) {
        this.authPermissionMapper = authPermissionMapper;
    }

    public Set<String> getUserPermissions(Long userId) {
        if (userId == null || userId <= 0) {
            return Collections.emptySet();
        }
        Set<String> permissions = authPermissionMapper.selectPermissionCodesByUserId(userId);
        return permissions == null ? Collections.emptySet() : permissions;
    }

    public void assignDefaultRole(Long userId) {
        if (userId != null && userId > 0) {
            authPermissionMapper.assignRoleByCode(userId, DEFAULT_USER_ROLE);
        }
    }
}
