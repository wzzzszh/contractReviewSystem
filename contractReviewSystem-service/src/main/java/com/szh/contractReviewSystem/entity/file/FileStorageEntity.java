package com.szh.contractReviewSystem.entity.file;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileStorageEntity {

    private Long id;

    private Long userId;

    private String fileName;

    private String filePath;

    private String fileCategory;

    private String fileStatus;

    private Long sourceFileId;

    private Long fileSize;

    private String contentType;

    private LocalDateTime expireTime;

    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
