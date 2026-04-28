package com.szh.contractReviewSystem.aspect;

import com.szh.contractReviewSystem.annotation.RequiresPermissions;
import com.szh.contractReviewSystem.exception.BusinessExceptionEnum;
import com.szh.contractReviewSystem.exception.CustomException;
import com.szh.contractReviewSystem.service.auth.PermissionService;
import com.szh.contractReviewSystem.utils.UserContextHolder;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

@Aspect
@Component
public class PermissionAspect {

    private static final Logger logger = LoggerFactory.getLogger(PermissionAspect.class);

    private final PermissionService permissionService;

    public PermissionAspect(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Pointcut("@annotation(com.szh.contractReviewSystem.annotation.RequiresPermissions) || "
            + "@within(com.szh.contractReviewSystem.annotation.RequiresPermissions)")
    public void permissionPointCut() {
    }

    @Before("permissionPointCut()")
    public void doBefore(JoinPoint joinPoint) {
        RequiresPermissions annotation = resolveAnnotation(joinPoint);
        if (annotation == null || annotation.value().length == 0) {
            return;
        }

        Long userId = UserContextHolder.getUserId();
        if (userId == null || userId <= 0) {
            throw new CustomException(BusinessExceptionEnum.UNAUTHORIZED);
        }

        Set<String> userPermissions = permissionService.getUserPermissions(userId);
        if (!hasPermission(annotation.value(), annotation.logical(), userPermissions)) {
            logger.warn("Permission denied, userId={}, required={}, actual={}",
                    userId, Arrays.toString(annotation.value()), userPermissions);
            throw new CustomException(BusinessExceptionEnum.FORBIDDEN);
        }
    }

    private RequiresPermissions resolveAnnotation(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermissions methodAnnotation = AnnotationUtils.findAnnotation(method, RequiresPermissions.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), RequiresPermissions.class);
    }

    private boolean hasPermission(String[] requiredPermissions,
                                  RequiresPermissions.Logical logical,
                                  Set<String> userPermissions) {
        if (logical == RequiresPermissions.Logical.OR) {
            return Arrays.stream(requiredPermissions).anyMatch(userPermissions::contains);
        }
        return Arrays.stream(requiredPermissions).allMatch(userPermissions::contains);
    }
}
