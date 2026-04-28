package com.szh.contractReviewSystem.mapper.auth;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

@Mapper
public interface AuthPermissionMapper {

    @Select("""
            SELECT DISTINCT p.permission_code
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id AND r.status = 1
            JOIN sys_role_permission rp ON rp.role_id = r.id
            JOIN sys_permission p ON p.id = rp.permission_id
            WHERE ur.user_id = #{userId}
            """)
    Set<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    @Insert("""
            INSERT IGNORE INTO sys_user_role (user_id, role_id)
            SELECT #{userId}, id
            FROM sys_role
            WHERE role_code = #{roleCode}
            """)
    int assignRoleByCode(@Param("userId") Long userId, @Param("roleCode") String roleCode);
}
