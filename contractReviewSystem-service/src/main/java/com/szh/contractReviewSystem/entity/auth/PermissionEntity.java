package com.szh.contractReviewSystem.entity.auth;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PermissionEntity {

    private Long id;

    private String permissionCode;

    private String permissionName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
