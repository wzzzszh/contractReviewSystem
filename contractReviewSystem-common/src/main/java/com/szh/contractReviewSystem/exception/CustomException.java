package com.szh.contractReviewSystem.exception;

import org.springframework.http.HttpStatus;

public class CustomException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    public CustomException(BusinessExceptionEnum error) {
        this(error, error.getMessage());
    }

    public CustomException(BusinessExceptionEnum error, String message) {
        super(message);
        this.code = error.getCode();
        this.message = message;
        this.httpStatus = error.getHttpStatus();
    }

    public CustomException(BusinessExceptionEnum error, String message, Throwable cause) {
        super(message, cause);
        this.code = error.getCode();
        this.message = message;
        this.httpStatus = error.getHttpStatus();
    }

    public CustomException(String message) {
        super(message);
        this.code = BusinessExceptionEnum.SYSTEM_ERROR.getCode();
        this.message = message;
        this.httpStatus = BusinessExceptionEnum.SYSTEM_ERROR.getHttpStatus();
    }

    public CustomException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
        this.httpStatus = resolveHttpStatus(code);
    }

    public CustomException(String message, Throwable cause) {
        super(message, cause);
        this.code = BusinessExceptionEnum.SYSTEM_ERROR.getCode();
        this.message = message;
        this.httpStatus = BusinessExceptionEnum.SYSTEM_ERROR.getHttpStatus();
    }

    public CustomException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
        this.httpStatus = resolveHttpStatus(code);
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    private HttpStatus resolveHttpStatus(int code) {
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
