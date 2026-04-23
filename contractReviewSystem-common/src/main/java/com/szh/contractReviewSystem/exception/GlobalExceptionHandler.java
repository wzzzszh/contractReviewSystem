package com.szh.contractReviewSystem.exception;

import com.szh.contractReviewSystem.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 全局异常处理器 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理自定义异常
     */
    @ExceptionHandler(CustomException.class)
    public Result<?> handleCustomException(CustomException e, HttpServletRequest request) {
        logger.error("请求地址: {}, 自定义异常: {}", request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    /**
     * 处理参数验证异常(MethodArgumentNotValidException)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder errorMsg = new StringBuilder();
        
        if (bindingResult.hasErrors()) {
            List<FieldError> fieldErrors = bindingResult.getFieldErrors();
            for (FieldError fieldError : fieldErrors) {
                errorMsg.append(fieldError.getDefaultMessage()).append(";");
            }
        }
        
        logger.error("请求地址: {}, 参数验证异常: {}", request.getRequestURI(), errorMsg.toString());
        return Result.error(BusinessExceptionEnum.PARAMETER_ERROR.getCode(), 
                          BusinessExceptionEnum.PARAMETER_ERROR.getMessage() + ":" + errorMsg.toString());
    }
    
    /**
     * 处理参数绑定异常(BindException)
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e, HttpServletRequest request) {
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder errorMsg = new StringBuilder();
        
        if (bindingResult.hasErrors()) {
            List<FieldError> fieldErrors = bindingResult.getFieldErrors();
            for (FieldError fieldError : fieldErrors) {
                errorMsg.append(fieldError.getDefaultMessage()).append(";");
            }
        }
        
        logger.error("请求地址: {}, 参数绑定异常: {}", request.getRequestURI(), errorMsg.toString());
        return Result.error(BusinessExceptionEnum.PARAMETER_ERROR.getCode(), 
                          BusinessExceptionEnum.PARAMETER_ERROR.getMessage() + ":" + errorMsg.toString());
    }
    
    /**
     * 处理系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        logger.error("请求地址: {}, 系统异常: ", request.getRequestURI(), e);
        return Result.error(BusinessExceptionEnum.SYSTEM_ERROR.getMessage());
    }
}