package com.szh.contractReviewSystem.service.db;

import com.szh.contractReviewSystem.entity.file.FileStorageEntity;
import com.szh.contractReviewSystem.entity.user.UserEntity;
import com.szh.contractReviewSystem.exception.BusinessExceptionEnum;
import com.szh.contractReviewSystem.exception.CustomException;
import com.szh.contractReviewSystem.mapper.file.FileStorageMapper;
import com.szh.contractReviewSystem.mapper.user.UserMapper;
import com.szh.contractReviewSystem.model.request.CreateFileStorageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileStorageRecordService {

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
        entity.setSourceFileId(sourceFileId);
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
}
