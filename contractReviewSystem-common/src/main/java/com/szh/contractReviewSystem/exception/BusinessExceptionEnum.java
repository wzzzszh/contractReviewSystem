package com.szh.contractReviewSystem.exception;

import org.springframework.http.HttpStatus;

public enum BusinessExceptionEnum {

    SYSTEM_ERROR(500, "系统错误", HttpStatus.INTERNAL_SERVER_ERROR),
    PARAMETER_ERROR(400, "参数错误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(401, "登录已失效，请重新登录", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "权限不足", HttpStatus.FORBIDDEN),
    NOT_FOUND(404, "资源不存在", HttpStatus.NOT_FOUND),

    USER_NOT_EXIST(10001, "用户不存在", HttpStatus.BAD_REQUEST),
    USER_ACCOUNT_EXIST(10002, "用户名已存在", HttpStatus.BAD_REQUEST),
    USER_PASSWORD_ERROR(10003, "密码错误", HttpStatus.BAD_REQUEST),
    USER_ACCOUNT_LOCKED(10004, "账号已被禁用", HttpStatus.FORBIDDEN),

    DATA_NOT_FOUND(20000, "数据不存在", HttpStatus.NOT_FOUND),
    DATA_INSERT_ERROR(20010, "数据插入失败", HttpStatus.INTERNAL_SERVER_ERROR),
    DATA_UPDATE_ERROR(20011, "数据更新失败", HttpStatus.INTERNAL_SERVER_ERROR),
    DATA_DELETE_ERROR(20012, "数据删除失败", HttpStatus.INTERNAL_SERVER_ERROR),

    FILE_NOT_FOUND(20001, "文件不存在", HttpStatus.NOT_FOUND),
    FILE_UPLOAD_FAILED(20002, "文件上传失败", HttpStatus.BAD_REQUEST),
    FILE_DOWNLOAD_FAILED(20003, "文件下载失败", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_FAILED(20004, "文件删除失败", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_TYPE_NOT_SUPPORTED(20005, "文件格式不支持", HttpStatus.BAD_REQUEST),
    FILE_ACCESS_DENIED(20006, "文件无访问权限", HttpStatus.FORBIDDEN),

    REVIEW_FAILED(30001, "合同审查失败", HttpStatus.INTERNAL_SERVER_ERROR),
    DOCX_MODIFY_FAILED(30002, "DOCX 修改失败", HttpStatus.INTERNAL_SERVER_ERROR),
    DOCUMENT_PARSE_FAILED(30003, "文档解析失败", HttpStatus.BAD_REQUEST),
    REVIEW_PARAMETER_ERROR(30004, "审查请求参数错误", HttpStatus.BAD_REQUEST),

    AI_CALL_FAILED(40001, "AI 服务调用失败", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_CALL_TIMEOUT(40002, "AI 服务调用超时", HttpStatus.GATEWAY_TIMEOUT),
    AI_RESPONSE_FORMAT_ERROR(40003, "AI 返回内容格式错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    BusinessExceptionEnum(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
