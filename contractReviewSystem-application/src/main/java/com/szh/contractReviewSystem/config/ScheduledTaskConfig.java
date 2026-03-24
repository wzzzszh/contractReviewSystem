package com.szh.parseModule.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executors;

/**
 * 定时任务配置类
 */
@Configuration
@EnableScheduling
public class ScheduledTaskConfig implements SchedulingConfigurer {
    
    /**
     * 配置定时任务线程池
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 设置定时任务线程池大小
        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(10));
    }
}