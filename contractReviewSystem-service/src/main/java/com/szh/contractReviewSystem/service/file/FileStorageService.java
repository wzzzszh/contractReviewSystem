package com.szh.contractReviewSystem.service.file;

import com.szh.contractReviewSystem.config.SystemConfig;
import com.szh.contractReviewSystem.utils.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
public class FileStorageService {

    private final SystemConfig systemConfig;

    public FileStorageService(SystemConfig systemConfig) {
        this.systemConfig = systemConfig;
    }

    public String upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String uploadPath = systemConfig == null ? null : systemConfig.getUploadPath();
        if (isBlank(uploadPath)) {
            return FileUtils.uploadFile(file);
        }
        return FileUtils.uploadFile(file, uploadPath);
    }

    public File requireDownloadFile(String filePath) {
        if (isBlank(filePath)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }

        File file = new File(filePath.trim());
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("文件不存在: " + filePath);
        }
        return file;
    }

    public boolean delete(String filePath) {
        if (isBlank(filePath)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        return FileUtils.deleteFile(filePath.trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
