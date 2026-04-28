package com.szh.contractReviewSystem.mapper.file;

import com.szh.contractReviewSystem.entity.file.FileStorageEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FileStorageMapper {

    @Insert("""
            INSERT INTO file_storage (user_id, file_name, file_path, file_category, source_file_id)
            VALUES (#{userId}, #{fileName}, #{filePath}, #{fileCategory}, #{sourceFileId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FileStorageEntity fileStorage);

    @Select("""
            SELECT id, user_id, file_name, file_path, file_category, source_file_id, create_time, update_time
            FROM file_storage
            WHERE id = #{id}
            """)
    FileStorageEntity selectById(@Param("id") Long id);

    @Select("""
            SELECT id, user_id, file_name, file_path, file_category, source_file_id, create_time, update_time
            FROM file_storage
            WHERE user_id = #{userId}
            ORDER BY id DESC
            """)
    List<FileStorageEntity> selectByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT id, user_id, file_name, file_path, file_category, source_file_id, create_time, update_time
            FROM file_storage
            WHERE source_file_id = #{sourceFileId}
            ORDER BY id DESC
            """)
    List<FileStorageEntity> selectBySourceFileId(@Param("sourceFileId") Long sourceFileId);
}
