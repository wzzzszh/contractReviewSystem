package com.szh.contractReviewSystem.model;

/**
 * 分页查询参数
 */
public class PageParam {
    
    /**
     * 当前页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页显示条数
     */
    private Integer pageSize = 10;
    
    /**
     * 排序字段
     */
    private String orderByColumn;
    
    /**
     * 排序方式 asc desc
     */
    private String isAsc = "asc";
    
    public Integer getPageNum() {
        return pageNum;
    }
    
    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }
    
    public Integer getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
    
    public String getOrderByColumn() {
        return orderByColumn;
    }
    
    public void setOrderByColumn(String orderByColumn) {
        this.orderByColumn = orderByColumn;
    }
    
    public String getIsAsc() {
        return isAsc;
    }
    
    public void setIsAsc(String isAsc) {
        this.isAsc = isAsc;
    }
}