package com.szh.contractReviewSystem.exception;

import com.szh.contractReviewSystem.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Result<?>> handleCustomException(CustomException e, HttpServletRequest request) {
        logger.warn("Business exception, uri={}, code={}, message={}",
                request.getRequestURI(), e.getCode(), e.getMessage());
        return build(e.getHttpStatus(), e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
                                                                           HttpServletRequest request) {
        String message = getBindingMessage(e.getBindingResult());
        logger.warn("Request body validation failed, uri={}, message={}", request.getRequestURI(), message);
        return build(BusinessExceptionEnum.PARAMETER_ERROR, message);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<?>> handleBindException(BindException e, HttpServletRequest request) {
        String message = getBindingMessage(e.getBindingResult());
        logger.warn("Parameter binding failed, uri={}, message={}", request.getRequestURI(), message);
        return build(BusinessExceptionEnum.PARAMETER_ERROR, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<?>> handleConstraintViolationException(ConstraintViolationException e,
                                                                        HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        logger.warn("Constraint validation failed, uri={}, message={}", request.getRequestURI(), message);
        return build(BusinessExceptionEnum.PARAMETER_ERROR, message);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<Result<?>> handleBadRequest(Exception e, HttpServletRequest request) {
        logger.warn("Bad request, uri={}, message={}", request.getRequestURI(), e.getMessage());
        return build(BusinessExceptionEnum.PARAMETER_ERROR, cleanMessage(e.getMessage()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<?>> handleNotFound(NoHandlerFoundException e, HttpServletRequest request) {
        logger.warn("Resource not found, uri={}", request.getRequestURI());
        return build(BusinessExceptionEnum.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<?>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                              HttpServletRequest request) {
        logger.warn("Method not supported, uri={}, method={}", request.getRequestURI(), e.getMethod());
        return build(HttpStatus.METHOD_NOT_ALLOWED, BusinessExceptionEnum.PARAMETER_ERROR.getCode(), "请求方法不支持");
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Result<?>> handleIOException(IOException e, HttpServletRequest request) {
        logger.error("IO exception, uri={}", request.getRequestURI(), e);
        return build(BusinessExceptionEnum.FILE_DOWNLOAD_FAILED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleException(Exception e, HttpServletRequest request) {
        logger.error("System exception, uri={}", request.getRequestURI(), e);
        return build(BusinessExceptionEnum.SYSTEM_ERROR);
    }

    private ResponseEntity<Result<?>> build(BusinessExceptionEnum error) {
        return build(error.getHttpStatus(), error.getCode(), error.getMessage());
    }

    private ResponseEntity<Result<?>> build(BusinessExceptionEnum error, String message) {
        String normalizedMessage = isBlank(message) ? error.getMessage() : message;
        return build(error.getHttpStatus(), error.getCode(), normalizedMessage);
    }

    private ResponseEntity<Result<?>> build(HttpStatus status, int code, String message) {
        return ResponseEntity.status(status).body(Result.error(code, message));
    }

    private String getBindingMessage(BindingResult bindingResult) {
        if (bindingResult == null || !bindingResult.hasErrors()) {
            return BusinessExceptionEnum.PARAMETER_ERROR.getMessage();
        }
        return bindingResult.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(message -> !isBlank(message))
                .collect(Collectors.joining("; "));
    }

    private String cleanMessage(String message) {
        return isBlank(message) ? BusinessExceptionEnum.PARAMETER_ERROR.getMessage() : message;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
