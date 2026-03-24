package com.szh.parseModule.controller;

import com.szh.parseModule.common.Result;
import com.szh.parseModule.exception.BusinessExceptionEnum;
import com.szh.parseModule.exception.CustomException;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 用户控制器示例 */
@RestController
@RequestMapping("/api/user")
public class UserController extends BaseController {
    
    /**
     * 获取用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public Result<String> getUser(@PathVariable @NotNull(message = "用户ID不能为空") Long id) {
        // 模拟业务逻辑
        if (id <= 0) {
            throw new CustomException(BusinessExceptionEnum.USER_NOT_EXIST.getCode(), 
                                    BusinessExceptionEnum.USER_NOT_EXIST.getMessage());
        }
        
        return success("用户信息: 用户ID=" + id);
    }
    
    /**
     * 创建用户
     *
     * @param username 用户名
     * @return 创建结果
     */
    @PostMapping
    public Result<String> createUser(@RequestParam @NotBlank(message = "用户名不能为空") String username) {
        // 模拟业务逻辑
        if ("admin".equals(username)) {
            throw new CustomException(BusinessExceptionEnum.USER_ACCOUNT_EXIST.getCode(), 
                                    BusinessExceptionEnum.USER_ACCOUNT_EXIST.getMessage());
        }
        
        return success("用户创建成功: " + username);
    }
    
    /**
     * 更新用户
     *
     * @param id 用户ID
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result<String> updateUser(@PathVariable @NotNull(message = "用户ID不能为空") Long id) {
        // 模拟业务逻辑
        return success("用户更新成功: " + id);
    }
    
    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteUser(@PathVariable @NotNull(message = "用户ID不能为空") Long id) {
        // 模拟业务逻辑
        if (id == 1) {
            throw new CustomException(BusinessExceptionEnum.FORBIDDEN.getCode(), 
                                    "不能删除管理员账户");
        }
        
        return success("用户删除成功: " + id);
    }
}