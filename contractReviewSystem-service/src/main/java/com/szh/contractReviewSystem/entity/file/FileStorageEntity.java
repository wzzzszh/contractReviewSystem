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

    private Long sourceFileId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
