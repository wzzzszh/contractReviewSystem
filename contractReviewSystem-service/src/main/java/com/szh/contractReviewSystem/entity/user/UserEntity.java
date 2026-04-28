package com.szh.contractReviewSystem.entity.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserEntity {

    private Long id;

    private String username;

    private String password;

    private String nickname;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
