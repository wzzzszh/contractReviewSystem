package com.szh.contractReviewSystem.model.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class CreateFileStorageRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotBlank(message = "文件地址不能为空")
    private String filePath;

    @NotBlank(message = "文件分类不能为空")
    private String fileCategory;

    private String fileStatus;

    private Long sourceFileId;

    private Long fileSize;

    private String contentType;

    private LocalDateTime expireTime;
}
