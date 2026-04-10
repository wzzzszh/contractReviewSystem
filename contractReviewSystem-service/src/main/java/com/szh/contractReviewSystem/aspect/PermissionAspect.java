package com.szh.contractReviewSystem.aspect;

import com.szh.contractReviewSystem.annotation.RequiresPermissions;
import com.szh.contractReviewSystem.exception.CustomException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 权限验证切面类 */
@Aspect
@Component
public class PermissionAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(PermissionAspect.class);
    
    /**
     * 配置织入点
     */
    @Pointcut("@annotation(com.szh.contractReviewSystem.annotation.RequiresPermissions)")
    public void permissionPointCut() {
    }
    
    /**
     * 前置通知
     */
    @Before("permissionPointCut()")
    public void doBefore(JoinPoint joinPoint) {
        // 获取方法上的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermissions annotation = method.getAnnotation(RequiresPermissions.class);
        
        if (annotation == null) {
            return;
        }
        
        // 获取注解中的权限信息
        String[] permissions = annotation.value();
        RequiresPermissions.Logical logical = annotation.logical();
        
        if (permissions == null || permissions.length == 0) {
            return;
        }
        
        // 模拟当前用户的权限（实际项目中应该从SecurityContext或Token中获取）
        Set<String> userPermissions = getCurrentUserPermissions();
        
        // 验证权限
        boolean hasPermission = checkPermission(permissions, logical, userPermissions);
        
        if (!hasPermission) {
            logger.warn("用户权限不足，所需权限: {}, 用户权限: {}", Arrays.toString(permissions), userPermissions);
            throw new CustomException(403, "权限不足，无法访问该资源");
        }
    }
    
    /**
     * 检查权限
     *
     * @param permissions    所需权限
     * @param logical        验证模式
     * @param userPermissions 用户拥有的权限
     * @return 是否有权限
     */
    private boolean checkPermission(String[] permissions, RequiresPermissions.Logical logical, Set<String> userPermissions) {
        if (logical == RequiresPermissions.Logical.AND) {
            // AND模式：必须拥有所有权限
            for (String permission : permissions) {
                if (!userPermissions.contains(permission)) {
                    return false;
                }
            }
            return true;
        } else {
            // OR模式：拥有其中一个权限即可
            for (String permission : permissions) {
                if (userPermissions.contains(permission)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    /**
     * 获取当前用户权限（模拟实现）
     *
     * @return 用户权限集合
     */
    private Set<String> getCurrentUserPermissions() {
        // 实际项目中应该从SecurityContext或Token中获取用户权限
        // 这里只是模拟实现
        Set<String> permissions = new HashSet<>();
        permissions.add("user:view");
        permissions.add("user:add");
        permissions.add("user:edit");
        return permissions;
    }
}