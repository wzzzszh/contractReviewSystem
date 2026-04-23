package com.szh.contractReviewSystem.controller;

import com.szh.contractReviewSystem.common.Result;
import com.szh.contractReviewSystem.service.file.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String filePath = fileStorageService.upload(file);
            return success("文件上传成功", filePath);
        } catch (Exception e) {
            return error("文件上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/download")
    public void downloadFile(@RequestParam("path") String filePath, HttpServletResponse response) {
        try {
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
        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage(), e);
        } catch (IOException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "文件下载失败", e);
        }
    }

    @DeleteMapping("/delete")
    public Result<String> deleteFile(@RequestParam("path") String filePath) {
        try {
            boolean result = fileStorageService.delete(filePath);
            if (result) {
                return success("文件删除成功");
            }
            return error("文件删除失败");
        } catch (Exception e) {
            return error("文件删除失败: " + e.getMessage());
        }
    }

    private void sendError(HttpServletResponse response, int status, String message, Exception exception) {
        logger.error(message, exception);
        if (response.isCommitted()) {
            return;
        }
        try {
            response.sendError(status, message);
        } catch (IOException ioException) {
            logger.error("发送错误响应失败", ioException);
        }
    }
}
