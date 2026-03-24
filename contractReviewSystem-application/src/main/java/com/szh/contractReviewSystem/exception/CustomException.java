package com.szh.contractReviewSystem.exception;

/**
 * 自定义异常类
 */
public class CustomException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    private int code;
    private String message;
    
    public CustomException(String message) {
        super(message);
        this.message = message;
        this.code = 500;
    }
    
    public CustomException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    public CustomException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.code = 500;
    }
    
    public CustomException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
}