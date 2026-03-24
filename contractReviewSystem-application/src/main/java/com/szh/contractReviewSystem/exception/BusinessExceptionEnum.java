package com.szh.parseModule.exception;

/**
 * 业务异常枚举类 */
public enum BusinessExceptionEnum {
    
    // 系统相关
    SYSTEM_ERROR(500, "系统错误"),
    PARAMETER_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    // 用户相关
    USER_NOT_EXIST(1001, "用户不存在"),
    USER_ACCOUNT_EXIST(1002, "账号已存在"),
    USER_PASSWORD_ERROR(1003, "密码错误"),
    USER_ACCOUNT_LOCKED(1004, "账号已被锁定"),
    // 数据相关
    DATA_NOT_FOUND(2001, "数据不存在"),
    DATA_INSERT_ERROR(2002, "数据插入失败"),
    DATA_UPDATE_ERROR(2003, "数据更新失败"),
    DATA_DELETE_ERROR(2004, "数据删除失败");
    private final int code;
    private final String message;
    BusinessExceptionEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}