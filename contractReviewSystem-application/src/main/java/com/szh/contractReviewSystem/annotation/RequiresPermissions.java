package com.szh.contractReviewSystem.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermissions {
    
    /**
     * 权限字符串数组
     */
    String[] value() default {};
    
    /**
     * 验证模式：AND表示必须满足所有权限，OR表示满足其中一个即可
     */
    Logical logical() default Logical.AND;
    
    /**
     * 验证模式枚举
     */
    enum Logical {
        AND, OR
    }
}