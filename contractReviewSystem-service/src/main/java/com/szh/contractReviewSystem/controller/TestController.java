package com.szh.contractReviewSystem.controller;

import com.szh.contractReviewSystem.common.Result;
import com.szh.contractReviewSystem.exception.CustomException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试控制器 */
@RestController
@RequestMapping("/api/test")
public class TestController extends BaseController {
    
    /**
     * 测试成功响应
     *
     * @return 成功响应
     */
    @GetMapping("/success")
    public Result<String> testSuccess() {
        return success("测试成功");
    }
    
    /**
     * 测试异常处理
     *
     * @return 异常响应
     */
    @GetMapping("/error")
    public Result<String> testError() {
        throw new CustomException("这是一个测试异常");
    }
    
    /**
     * 测试业务异常
     *
     * @return 业务异常响应
     */
    @GetMapping("/business-error")
    public Result<String> testBusinessError() {
        throw new CustomException(1001, "用户不存在");
    }
}