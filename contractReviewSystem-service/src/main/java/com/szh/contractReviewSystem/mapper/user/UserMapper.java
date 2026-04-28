package com.szh.contractReviewSystem.mapper.user;

import com.szh.contractReviewSystem.entity.user.UserEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {

    @Insert("""
            INSERT INTO sys_user (username, password, nickname, status)
            VALUES (#{username}, #{password}, #{nickname}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserEntity user);

    @Select("""
            SELECT id, username, password, nickname, status, create_time, update_time
            FROM sys_user
            WHERE id = #{id}
            """)
    UserEntity selectById(@Param("id") Long id);

    @Select("""
            SELECT id, username, password, nickname, status, create_time, update_time
            FROM sys_user
            WHERE username = #{username}
            LIMIT 1
            """)
    UserEntity selectByUsername(@Param("username") String username);

    @Select("""
            SELECT id, username, password, nickname, status, create_time, update_time
            FROM sys_user
            ORDER BY id DESC
            """)
    List<UserEntity> selectAll();

    @Update("""
            UPDATE sys_user
            SET password = #{password}
            WHERE id = #{id}
            """)
    int updatePassword(@Param("id") Long id, @Param("password") String password);
}
