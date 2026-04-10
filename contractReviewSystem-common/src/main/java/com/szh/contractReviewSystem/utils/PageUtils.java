package com.szh.contractReviewSystem.utils;

import java.util.List;

/**
 * 分页工具类
 */
public class PageUtils {
    
    /**
     * 创建分页结果
     *
     * @param list 数据列表
     * @param total 总记录数
     * @param pageNum 当前页码
     * @param pageSize 每页大小
     * @param <T> 数据类型
     * @return 分页结果
     */
    public static <T> com.szh.contractReviewSystem.common.PageResult<T> createPageResult(List<T> list, Long total, 
                                                                       Integer pageNum, Integer pageSize) {
        return com.szh.contractReviewSystem.common.PageResult.success(list, total, pageNum, pageSize);
    }
    
    /**
     * 计算总页数
     *
     * @param total 总记录数
     * @param pageSize 每页大小
     * @return 总页数
     */
    public static int calculateTotalPages(long total, int pageSize) {
        if (pageSize <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / pageSize);
    }
    
    /**
     * 计算起始位置
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 起始位置
     */
    public static int calculateStartIndex(int pageNum, int pageSize) {
        if (pageNum <= 0 || pageSize <= 0) {
            return 0;
        }
        return (pageNum - 1) * pageSize;
    }
}