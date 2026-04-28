package com.szh.contractReviewSystem.service.db;

import com.szh.contractReviewSystem.entity.file.FileStorageEntity;
import com.szh.contractReviewSystem.entity.user.UserEntity;
import com.szh.contractReviewSystem.exception.BusinessExceptionEnum;
import com.szh.contractReviewSystem.exception.CustomException;
import com.szh.contractReviewSystem.mapper.file.FileStorageMapper;
import com.szh.contractReviewSystem.mapper.user.UserMapper;
import com.szh.contractReviewSystem.model.request.CreateFileStorageRequest;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FileStorageRecordService {

    private static final String DEFAULT_FILE_STATUS = "active";
    private static final String TEMP_FILE_STATUS = "temp";
    private static final String AGENT_WORK_CATEGORY = "agent_work";

    private final FileStorageMapper fileStorageMapper;
    private final UserMapper userMapper;

    public FileStorageRecordService(FileStorageMapper fileStorageMapper, UserMapper userMapper) {
        this.fileStorageMapper = fileStorageMapper;
        this.userMapper = userMapper;
    }

    public FileStorageEntity createRecord(CreateFileStorageRequest request) {
        if (request == null) {
            throw new CustomException(BusinessExceptionEnum.PARAMETER_ERROR, "文件存储请求不能为空");
        }
        Long userId = requireExistingUserId(request.getUserId());

        Long sourceFileId = request.getSourceFileId();
        if (sourceFileId != null && sourceFileId > 0 && fileStorageMapper.selectById(sourceFileId) == null) {
            throw new CustomException(BusinessExceptionEnum.FILE_NOT_FOUND, "原始文件记录不存在");
        }

        FileStorageEntity entity = new FileStorageEntity();
        entity.setUserId(userId);
        entity.setFileName(requireText(request.getFileName(), "文件名不能为空"));
        entity.setFilePath(requireText(request.getFilePath(), "文件地址不能为空"));
        entity.setFileCategory(requireText(request.getFileCategory(), "文件分类不能为空"));
        entity.setFileStatus(defaultText(request.getFileStatus(), DEFAULT_FILE_STATUS));
        entity.setSourceFileId(sourceFileId);
        entity.setFileSize(defaultFileSize(request.getFileSize(), entity.getFilePath()));
        entity.setContentType(defaultText(request.getContentType(), probeContentType(entity.getFilePath())));
        entity.setExpireTime(request.getExpireTime());
        entity.setDeleted(0);
        fileStorageMapper.insert(entity);
        return fileStorageMapper.selectById(entity.getId());
    }

    public FileStorageEntity createUploadedFileRecord(Long userId, String fileName, String filePath) {
        CreateFileStorageRequest request = new CreateFileStorageRequest();
        request.setUserId(requireExistingUserId(userId));
        request.setFileName(requireText(fileName, "文件名不能为空"));
        request.setFilePath(requireText(filePath, "文件地址不能为空"));
        request.setFileCategory("uploaded");
        return createRecord(request);
    }

    public FileStorageEntity createUploadedFileRecord(Long userId,
                                                      String fileName,
                                                      String filePath,
                                                      Long fileSize,
                                                      String contentType) {
        CreateFileStorageRequest request = new CreateFileStorageRequest();
        request.setUserId(requireExistingUserId(userId));
        request.setFileName(requireText(fileName, "文件名不能为空"));
        request.setFilePath(requireText(filePath, "文件地址不能为空"));
        request.setFileCategory("uploaded");
        request.setFileSize(fileSize);
        request.setContentType(contentType);
        return createRecord(request);
    }

    public FileStorageEntity createModifiedFileRecord(Long userId,
                                                      String fileName,
                                                      String filePath,
                                                      Long sourceFileId) {
        CreateFileStorageRequest request = new CreateFileStorageRequest();
        request.setUserId(requireExistingUserId(userId));
        request.setFileName(requireText(fileName, "fileName must not be blank"));
        request.setFilePath(requireText(filePath, "filePath must not be blank"));
        request.setFileCategory("modified");
        request.setSourceFileId(sourceFileId);
        request.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        return createRecord(request);
    }

    public FileStorageEntity createAgentWorkRecord(Long userId, Path workDirectory, LocalDateTime expireTime) {
        CreateFileStorageRequest request = new CreateFileStorageRequest();
        request.setUserId(requireExistingUserId(userId));
        request.setFileName(workDirectory.getFileName().toString());
        request.setFilePath(workDirectory.toAbsolutePath().normalize().toString());
        request.setFileCategory(AGENT_WORK_CATEGORY);
        request.setFileStatus(TEMP_FILE_STATUS);
        request.setExpireTime(expireTime);
        return createRecord(request);
    }

    public FileStorageEntity getById(Long id) {
        if (id == null || id <= 0) {
            throw new CustomException(BusinessExceptionEnum.PARAMETER_ERROR, "文件记录ID不能为空且必须大于0");
        }
        FileStorageEntity entity = fileStorageMapper.selectById(id);
        if (entity == null) {
            throw new CustomException(BusinessExceptionEnum.FILE_NOT_FOUND);
        }
        return entity;
    }

    public FileStorageEntity getOwnedActiveRecord(Long id, Long currentUserId) {
        FileStorageEntity entity = getById(id);
        if (currentUserId == null || !currentUserId.equals(entity.getUserId())) {
            throw new CustomException(BusinessExceptionEnum.FILE_ACCESS_DENIED);
        }
        return entity;
    }

    public void softDeleteOwnedRecord(Long id, Long currentUserId) {
        getOwnedActiveRecord(id, currentUserId);
        fileStorageMapper.softDeleteById(id);
    }

    public void softDeleteRecord(Long id) {
        if (id != null && id > 0) {
            fileStorageMapper.softDeleteById(id);
        }
    }

    public List<FileStorageEntity> listExpiredTempRecords(LocalDateTime now) {
        return fileStorageMapper.selectExpiredTempRecords(now);
    }

    public List<FileStorageEntity> listByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new CustomException(BusinessExceptionEnum.PARAMETER_ERROR, "用户ID不能为空且必须大于0");
        }
        return fileStorageMapper.selectByUserId(userId);
    }

    public List<FileStorageEntity> listBySourceFileId(Long sourceFileId) {
        if (sourceFileId == null || sourceFileId <= 0) {
            throw new CustomException(BusinessExceptionEnum.PARAMETER_ERROR, "原始文件ID不能为空且必须大于0");
        }
        return fileStorageMapper.selectBySourceFileId(sourceFileId);
    }

    private Long requireExistingUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new CustomException(BusinessExceptionEnum.PARAMETER_ERROR, "用户ID不能为空且必须大于0");
        }
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new CustomException(BusinessExceptionEnum.USER_NOT_EXIST);
        }
        return userId;
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new CustomException(BusinessExceptionEnum.PARAMETER_ERROR, message);
        }
        return value.trim();
    }

    private String defaultText(String value, String defaultValue) {
        String normalized = normalizeText(value);
        return normalized == null ? defaultValue : normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Long normalizeFileSize(Long fileSize) {
        return fileSize == null || fileSize < 0 ? null : fileSize;
    }

    private Long defaultFileSize(Long fileSize, String filePath) {
        Long normalized = normalizeFileSize(fileSize);
        if (normalized != null) {
            return normalized;
        }
        try {
            Path path = Path.of(filePath);
            return Files.exists(path) && !Files.isDirectory(path) ? Files.size(path) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String probeContentType(String filePath) {
        try {
            if (filePath == null) {
                return null;
            }
            Path path = Path.of(filePath);
            if (!Files.exists(path) || Files.isDirectory(path)) {
                return null;
            }
            return Files.probeContentType(path);
        } catch (Exception ignored) {
            return null;
        }
    }
}
