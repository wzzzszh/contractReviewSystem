package com.szh.contractReviewSystem.controller;

import com.szh.contractReviewSystem.annotation.RequiresPermissions;
import com.szh.contractReviewSystem.common.Result;
import com.szh.contractReviewSystem.controller.notdb.BaseController;
import com.szh.contractReviewSystem.entity.file.FileStorageEntity;
import com.szh.contractReviewSystem.exception.BusinessExceptionEnum;
import com.szh.contractReviewSystem.exception.CustomException;
import com.szh.contractReviewSystem.model.request.CreateFileStorageRequest;
import com.szh.contractReviewSystem.service.db.FileStorageRecordService;
import com.szh.contractReviewSystem.service.file.FileStorageService;
import com.szh.contractReviewSystem.utils.UserContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;

@RestController
@RequestMapping("/api/db/files")
public class FileStorageRecordController extends BaseController {

    private final FileStorageRecordService fileStorageRecordService;

    private final FileStorageService fileStorageService;

    public FileStorageRecordController(FileStorageRecordService fileStorageRecordService,
                                       FileStorageService fileStorageService) {
        this.fileStorageRecordService = fileStorageRecordService;
        this.fileStorageService = fileStorageService;
    }

    @RequiresPermissions("file:record:create")
    @PostMapping
    public Result<FileStorageEntity> createRecord(@Valid @RequestBody CreateFileStorageRequest request) {
        if (!UserContextHolder.requireUserId().equals(request.getUserId())) {
            throw new CustomException(BusinessExceptionEnum.FILE_ACCESS_DENIED);
        }
        return success("文件地址记录创建成功", fileStorageRecordService.createRecord(request));
    }

    @RequiresPermissions("file:list")
    @GetMapping("/{id}")
    public Result<FileStorageEntity> getById(@PathVariable @NotNull(message = "文件记录ID不能为空") Long id) {
        return success(fileStorageRecordService.getById(id));
    }

    @RequiresPermissions("file:download")
    @GetMapping("/{id}/download")
    public void downloadById(@PathVariable @NotNull(message = "文件记录ID不能为空") Long id,
                             HttpServletResponse response) throws IOException {
        FileStorageEntity record = fileStorageRecordService.getOwnedActiveRecord(id, UserContextHolder.requireUserId());
        File file = fileStorageService.requireManagedDownloadFile(record);
        response.setContentType(defaultContentType(record.getContentType()));
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(record.getFileName(), "UTF-8")
        );
        response.setContentLengthLong(file.length());

        try (FileInputStream inputStream = new FileInputStream(file);
             OutputStream outputStream = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
        }
    }

    @RequiresPermissions("file:delete")
    @DeleteMapping("/{id}")
    public Result<String> softDeleteById(@PathVariable @NotNull(message = "文件记录ID不能为空") Long id) {
        fileStorageRecordService.softDeleteOwnedRecord(id, UserContextHolder.requireUserId());
        return success("文件记录已删除");
    }

    @RequiresPermissions("file:list")
    @GetMapping("/user/{userId}")
    public Result<List<FileStorageEntity>> listByUserId(@PathVariable @NotNull(message = "用户ID不能为空") Long userId) {
        return success(fileStorageRecordService.listByUserId(userId));
    }

    @RequiresPermissions("file:list")
    @GetMapping("/source/{sourceFileId}")
    public Result<List<FileStorageEntity>> listBySourceFileId(
            @PathVariable @NotNull(message = "原始文件ID不能为空") Long sourceFileId) {
        return success(fileStorageRecordService.listBySourceFileId(sourceFileId));
    }

    private String defaultContentType(String contentType) {
        return contentType == null || contentType.trim().isEmpty()
                ? "application/octet-stream"
                : contentType.trim();
    }
}
