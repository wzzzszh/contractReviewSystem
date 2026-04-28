package com.szh.contractReviewSystem.service.file;

import com.szh.contractReviewSystem.config.SystemConfig;
import com.szh.contractReviewSystem.entity.file.FileStorageEntity;
import com.szh.contractReviewSystem.exception.BusinessExceptionEnum;
import com.szh.contractReviewSystem.exception.CustomException;
import com.szh.contractReviewSystem.utils.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;

@Service
public class FileStorageService {

    private final SystemConfig systemConfig;

    public FileStorageService(SystemConfig systemConfig) {
        this.systemConfig = systemConfig;
    }

    public String upload(MultipartFile file) {
        validateUploadFile(file);
        return saveUploadedFile(file, resolveUploadRoot());
    }

    public String uploadForUser(Long userId, MultipartFile file) {
        if (userId == null || userId <= 0) {
            throw new CustomException(BusinessExceptionEnum.PARAMETER_ERROR, "用户ID不能为空且必须大于0");
        }
        validateUploadFile(file);

        Path userDirectory = resolveUploadRoot().resolve("user-" + userId);
        return saveUploadedFile(file, userDirectory);
    }

    public File requireDownloadFile(String filePath) {
        if (isBlank(filePath)) {
            throw new CustomException(BusinessExceptionEnum.PARAMETER_ERROR, "文件路径不能为空");
        }

        Path path = normalizeManagedPath(filePath);
        File file = path.toFile();
        if (!file.exists() || !file.isFile()) {
            throw new CustomException(BusinessExceptionEnum.FILE_NOT_FOUND);
        }
        return file;
    }

    public File requireManagedDownloadFile(FileStorageEntity record) {
        if (record == null || record.getDeleted() != null && record.getDeleted() == 1) {
            throw new CustomException(BusinessExceptionEnum.FILE_NOT_FOUND);
        }
        Path filePath = normalizeManagedPath(record.getFilePath());
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new CustomException(BusinessExceptionEnum.FILE_NOT_FOUND);
        }
        return filePath.toFile();
    }

    public boolean delete(String filePath) {
        if (isBlank(filePath)) {
            throw new CustomException(BusinessExceptionEnum.PARAMETER_ERROR, "文件路径不能为空");
        }
        return FileUtils.deleteFile(normalizeManagedPath(filePath).toString());
    }

    public void deleteManagedPath(String rawPath) {
        Path path = normalizeManagedPath(rawPath);
        if (!Files.exists(path)) {
            return;
        }
        try {
            if (Files.isDirectory(path)) {
                try (var stream = Files.walk(path)) {
                    stream.sorted(Comparator.reverseOrder())
                            .filter(item -> !item.equals(path) || isManagedPath(item))
                            .forEach(this::deleteIfExists);
                }
                return;
            }
            deleteIfExists(path);
        } catch (IOException e) {
            throw new CustomException(BusinessExceptionEnum.FILE_DELETE_FAILED,
                    BusinessExceptionEnum.FILE_DELETE_FAILED.getMessage(), e);
        }
    }

    public Path getUploadRoot() {
        return resolveUploadRoot();
    }

    public Path normalizeManagedPath(String rawPath) {
        if (isBlank(rawPath)) {
            throw new CustomException(BusinessExceptionEnum.PARAMETER_ERROR, "文件路径不能为空");
        }
        Path path = Path.of(rawPath.trim()).toAbsolutePath().normalize();
        if (!isManagedPath(path)) {
            throw new CustomException(BusinessExceptionEnum.FILE_ACCESS_DENIED);
        }
        return path;
    }

    private String saveUploadedFile(MultipartFile file, Path directory) {
        String originalFilename = file.getOriginalFilename();
        if (!FileUtils.isValidExtension(originalFilename)) {
            throw new CustomException(BusinessExceptionEnum.FILE_TYPE_NOT_SUPPORTED);
        }

        try {
            Files.createDirectories(directory);
            Path targetPath = directory
                    .resolve(FileUtils.generateUniqueFileName(originalFilename))
                    .toAbsolutePath()
                    .normalize();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return targetPath.toString();
        } catch (IOException e) {
            throw new CustomException(BusinessExceptionEnum.FILE_UPLOAD_FAILED,
                    BusinessExceptionEnum.FILE_UPLOAD_FAILED.getMessage(), e);
        }
    }

    private Path resolveUploadRoot() {
        String uploadPath = systemConfig == null ? null : systemConfig.getUploadPath();
        if (isBlank(uploadPath)) {
            return Path.of(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
        }

        String normalizedPath = uploadPath.trim();
        Path configuredPath = Path.of(normalizedPath);
        if (isExplicitAbsolutePath(normalizedPath, configuredPath)) {
            return configuredPath.toAbsolutePath().normalize();
        }

        String relativePath = trimLeadingSeparators(normalizedPath);
        if (isBlank(relativePath)) {
            relativePath = "uploads";
        }
        return Path.of(System.getProperty("user.dir"), relativePath).toAbsolutePath().normalize();
    }

    private boolean isManagedPath(Path path) {
        Path normalizedPath = path.toAbsolutePath().normalize();
        return managedRoots().stream().anyMatch(root -> isSameOrChild(root, normalizedPath));
    }

    private List<Path> managedRoots() {
        return List.of(
                getUploadRoot(),
                Path.of(System.getProperty("user.dir"), "work").toAbsolutePath().normalize(),
                Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize()
        );
    }

    private boolean isSameOrChild(Path root, Path path) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        return path.equals(normalizedRoot) || path.startsWith(normalizedRoot);
    }

    private void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new CustomException(BusinessExceptionEnum.FILE_DELETE_FAILED,
                    BusinessExceptionEnum.FILE_DELETE_FAILED.getMessage(), e);
        }
    }

    private boolean isExplicitAbsolutePath(String rawPath, Path path) {
        if (!path.isAbsolute()) {
            return false;
        }
        if (File.separatorChar != '\\') {
            return true;
        }
        return rawPath.matches("^[a-zA-Z]:[\\\\/].*") || rawPath.startsWith("\\\\");
    }

    private String trimLeadingSeparators(String value) {
        String result = value;
        while (result.startsWith("/") || result.startsWith("\\")) {
            result = result.substring(1);
        }
        return result;
    }

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(BusinessExceptionEnum.PARAMETER_ERROR, "文件不能为空");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
