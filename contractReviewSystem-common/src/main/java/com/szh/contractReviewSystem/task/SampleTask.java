package com.szh.contractReviewSystem.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 示例定时任务类
 */
@Component
public class SampleTask {
    
    private static final Logger logger = LoggerFactory.getLogger(SampleTask.class);
    
    /**
     * 每5秒执行一次的任务
     */
    @Scheduled(fixedRate = 500000)
    public void fixedRateTask() {
        logger.info("每500秒执行一次的任务 - 当前时间: {}",
                   LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
    
    /**
     * 每分钟执行一次的任务
     */
    @Scheduled(cron = "0 * * * * ?")
    public void cronTask() {
        logger.info("每分钟执行一次的任务 - 当前时间: {}", 
                   LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
    
    /**
     * 每天凌晨2点执行的任务
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyTask() {
        logger.info("每天凌晨2点执行的任务 - 当前时间: {}", 
                   LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
}