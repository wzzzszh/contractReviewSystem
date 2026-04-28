package com.szh.contractReviewSystem.entity.auth;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoleEntity {

    private Long id;

    private String roleCode;

    private String roleName;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
