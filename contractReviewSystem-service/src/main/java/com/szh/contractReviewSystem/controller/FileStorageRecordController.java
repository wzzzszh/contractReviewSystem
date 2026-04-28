package com.szh.contractReviewSystem.controller;

import com.szh.contractReviewSystem.annotation.RequiresPermissions;
import com.szh.contractReviewSystem.common.Result;
import com.szh.contractReviewSystem.controller.notdb.BaseController;
import com.szh.contractReviewSystem.entity.file.FileStorageEntity;
import com.szh.contractReviewSystem.model.request.CreateFileStorageRequest;
import com.szh.contractReviewSystem.service.db.FileStorageRecordService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/api/db/files")
public class FileStorageRecordController extends BaseController {

    private final FileStorageRecordService fileStorageRecordService;

    public FileStorageRecordController(FileStorageRecordService fileStorageRecordService) {
        this.fileStorageRecordService = fileStorageRecordService;
    }

    @RequiresPermissions("file:record:create")
    @PostMapping
    public Result<FileStorageEntity> createRecord(@Valid @RequestBody CreateFileStorageRequest request) {
        return success("文件地址记录创建成功", fileStorageRecordService.createRecord(request));
    }

    @RequiresPermissions("file:list")
    @GetMapping("/{id}")
    public Result<FileStorageEntity> getById(@PathVariable @NotNull(message = "文件记录ID不能为空") Long id) {
        return success(fileStorageRecordService.getById(id));
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
}
