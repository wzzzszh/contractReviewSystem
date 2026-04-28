package com.szh.contractReviewSystem.controller.notdb;

import com.szh.contractReviewSystem.annotation.RequiresPermissions;
import com.szh.contractReviewSystem.common.Result;
import com.szh.contractReviewSystem.entity.file.FileStorageEntity;
import com.szh.contractReviewSystem.exception.BusinessExceptionEnum;
import com.szh.contractReviewSystem.exception.CustomException;
import com.szh.contractReviewSystem.service.db.FileStorageRecordService;
import com.szh.contractReviewSystem.service.file.FileStorageService;
import com.szh.contractReviewSystem.utils.UserContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

@RestController
@RequestMapping("/api/file")
public class FileController extends BaseController {

    private final FileStorageService fileStorageService;
    private final FileStorageRecordService fileStorageRecordService;

    public FileController(FileStorageService fileStorageService,
                          FileStorageRecordService fileStorageRecordService) {
        this.fileStorageService = fileStorageService;
        this.fileStorageRecordService = fileStorageRecordService;
    }

    @RequiresPermissions("file:upload")
    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String filePath = fileStorageService.upload(file);
        return success("文件上传成功", filePath);
    }

    @RequiresPermissions("file:upload")
    @PostMapping("/upload/current-user")
    public Result<FileStorageEntity> uploadFileForCurrentUser(@RequestParam("file") MultipartFile file) {
        Long userId = UserContextHolder.requireUserId();
        String filePath = fileStorageService.uploadForUser(userId, file);
        String originalFilename = file == null ? null : file.getOriginalFilename();
        FileStorageEntity record =
                fileStorageRecordService.createUploadedFileRecord(userId, originalFilename, filePath);
        return success("当前用户文件上传成功", record);
    }

    @RequiresPermissions("file:download")
    @GetMapping("/download")
    public void downloadFile(@RequestParam("path") String filePath, HttpServletResponse response) throws IOException {
        File file = fileStorageService.requireDownloadFile(filePath);
        response.setContentType("application/octet-stream");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(file.getName(), "UTF-8")
        );
        response.setContentLengthLong(file.length());

        try (FileInputStream inputStream = new FileInputStream(file);
             OutputStream outputStream = response.getOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
        }
    }

    @RequiresPermissions("file:delete")
    @DeleteMapping("/delete")
    public Result<String> deleteFile(@RequestParam("path") String filePath) {
        if (fileStorageService.delete(filePath)) {
            return success("文件删除成功");
        }
        throw new CustomException(BusinessExceptionEnum.FILE_DELETE_FAILED);
    }
}
