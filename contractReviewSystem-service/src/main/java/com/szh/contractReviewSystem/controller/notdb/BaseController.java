package com.szh.contractReviewSystem.controller.notdb;

import com.szh.contractReviewSystem.common.PageResult;
import com.szh.contractReviewSystem.common.Result;
import com.szh.contractReviewSystem.exception.CustomException;

import java.util.List;

/**
 * 基础控制器类
 */
public class BaseController {
    
    /**
     * 响应返回结果
     *
     * @param rows 影响行数
     * @return 操作结果
     */
    protected Result<?> toResult(int rows) {
        return rows > 0 ? Result.success() : Result.error();
    }
    
    /**
     * 响应返回结果
     *
     * @param result 结果
     * @return 操作结果
     */
    protected Result<?> toResult(boolean result) {
        return result ? Result.success() : Result.error();
    }
    
    /**
     * 响应返回结果
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return 操作结果
     */
    protected <T> Result<T> success(T data) {
        return Result.success(data);
    }
    
    /**
     * 响应返回结果
     *
     * @param <T> 数据类型
     * @return 操作结果
     */
    protected <T> Result<T> success() {
        return Result.success();
    }
    
    /**
     * 响应返回结果
     *
     * @param msg  消息内容
     * @param data 数据
     * @param <T>  数据类型
     * @return 操作结果
     */
    protected <T> Result<T> success(String msg, T data) {
        return Result.success(msg, data);
    }
    
    /**
     * 响应返回错误
     *
     * @param msg 消息内容
     * @return 操作结果
     */
    protected <T> Result<T> error(String msg) {
        return Result.error(msg);
    }
    
    /**
     * 响应返回错误
     *
     * @param code 错误码
     * @param msg  消息内容
     * @return 操作结果
     */
    protected <T> Result<T> error(int code, String msg) {
        return Result.error(code, msg);
    }
    
    /**
     * 抛出异常
     *
     * @param msg 消息内容
     */
    protected void throwError(String msg) {
        throw new CustomException(msg);
    }
    
    /**
     * 抛出异常
     *
     * @param code 错误码
     * @param msg  消息内容
     */
    protected void throwError(int code, String msg) {
        throw new CustomException(code, msg);
    }
    
    /**
     * 分页返回结果
     *
     * @param list     数据列表
     * @param total    总记录数
     * @param pageNum  当前页码
     * @param pageSize 每页显示条数
     * @param <T>      数据类型
     * @return 分页结果
     */
    protected <T> PageResult<T> pageResult(List<T> list, Long total, Integer pageNum, Integer pageSize) {
        return PageResult.success(list, total, pageNum, pageSize);
    }
}