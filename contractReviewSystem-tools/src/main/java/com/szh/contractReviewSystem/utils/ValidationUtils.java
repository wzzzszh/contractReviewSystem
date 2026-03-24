package com.szh.parseModule.utils;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

/**
 * 楠岃瘉宸ュ叿绫� */
public class ValidationUtils {
    
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    
    /**
     * 楠岃瘉瀵硅薄
     */
    public static <T> Set<ConstraintViolation<T>> validate(T obj) {
        return validator.validate(obj);
    }
    
    /**
     * 楠岃瘉瀵硅薄骞舵姏鍑哄紓甯�
     */
    public static <T> void validateAndThrow(T obj) {
        Set<ConstraintViolation<T>> violations = validate(obj);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<T> violation : violations) {
                sb.append(violation.getMessage()).append(";");
            }
            throw new IllegalArgumentException(sb.toString());
        }
    }
    
    /**
     * 楠岃瘉瀵硅薄骞惰繑鍥為敊璇俊鎭�
     */
    public static <T> String validateAndGetMessage(T obj) {
        Set<ConstraintViolation<T>> violations = validate(obj);
        if (violations.isEmpty()) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        for (ConstraintViolation<T> violation : violations) {
            sb.append(violation.getMessage()).append(";");
        }
        return sb.toString();
    }
    
    /**
     * 楠岃瘉瀛楃涓叉槸鍚︿负绌�
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 楠岃瘉瀛楃涓叉槸鍚︿笉涓虹┖
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * 楠岃瘉闆嗗悎鏄惁涓虹┖
     */
    public static boolean isEmpty(Iterable<?> iterable) {
        return iterable == null || !iterable.iterator().hasNext();
    }
    
    /**
     * 楠岃瘉闆嗗悎鏄惁涓嶄负绌�
     */
    public static boolean isNotEmpty(Iterable<?> iterable) {
        return !isEmpty(iterable);
    }
    
    /**
     * 楠岃瘉瀵硅薄鏄惁涓虹┖
     */
    public static boolean isEmpty(Object obj) {
        return obj == null;
    }
    
    /**
     * 楠岃瘉瀵硅薄鏄惁涓嶄负绌�
     */
    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }
}