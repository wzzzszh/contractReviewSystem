package com.szh.contractReviewSystem.mapper.file;

import com.szh.contractReviewSystem.entity.file.FileStorageEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FileStorageMapper {

    @Insert("""
            INSERT INTO file_storage (
                user_id, file_name, file_path, file_category, file_status,
                source_file_id, file_size, content_type, expire_time, deleted
            )
            VALUES (
                #{userId}, #{fileName}, #{filePath}, #{fileCategory}, #{fileStatus},
                #{sourceFileId}, #{fileSize}, #{contentType}, #{expireTime}, #{deleted}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FileStorageEntity fileStorage);

    @Select("""
            SELECT id, user_id, file_name, file_path, file_category, file_status,
                   source_file_id, file_size, content_type, expire_time, deleted, create_time, update_time
            FROM file_storage
            WHERE id = #{id} AND deleted = 0
            """)
    FileStorageEntity selectById(@Param("id") Long id);

    @Select("""
            SELECT id, user_id, file_name, file_path, file_category, file_status,
                   source_file_id, file_size, content_type, expire_time, deleted, create_time, update_time
            FROM file_storage
            WHERE user_id = #{userId}
              AND deleted = 0
              AND file_status = 'active'
            ORDER BY id DESC
            """)
    List<FileStorageEntity> selectByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT id, user_id, file_name, file_path, file_category, file_status,
                   source_file_id, file_size, content_type, expire_time, deleted, create_time, update_time
            FROM file_storage
            WHERE source_file_id = #{sourceFileId}
              AND deleted = 0
              AND file_status = 'active'
            ORDER BY id DESC
            """)
    List<FileStorageEntity> selectBySourceFileId(@Param("sourceFileId") Long sourceFileId);

    @Select("""
            SELECT id, user_id, file_name, file_path, file_category, file_status,
                   source_file_id, file_size, content_type, expire_time, deleted, create_time, update_time
            FROM file_storage
            WHERE deleted = 0
              AND file_status IN ('temp', 'agent_work', 'expired')
              AND expire_time IS NOT NULL
              AND expire_time <= #{now}
            ORDER BY id ASC
            """)
    List<FileStorageEntity> selectExpiredTempRecords(@Param("now") LocalDateTime now);

    @Update("""
            UPDATE file_storage
            SET deleted = 1, file_status = 'deleted'
            WHERE id = #{id}
            """)
    int softDeleteById(@Param("id") Long id);
}
