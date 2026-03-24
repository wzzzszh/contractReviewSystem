package com.szh.parseModule.controller;

import com.szh.parseModule.common.Result;
import com.szh.parseModule.config.SystemConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统监控控制器
 */
@RestController
@RequestMapping("/api/monitor")
public class SystemMonitorController extends BaseController {
    
    @Autowired
    private SystemConfig systemConfig;
    
    /**
     * 获取系统信息
     *
     * @return 系统信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // 系统配置信息
        info.put("appName", systemConfig.getName());
        info.put("appVersion", systemConfig.getVersion());
        info.put("copyrightYear", systemConfig.getCopyrightYear());
        
        // JVM信息
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        info.put("jvmName", runtimeMXBean.getVmName());
        info.put("jvmVersion", runtimeMXBean.getVmVersion());
        info.put("jvmVendor", runtimeMXBean.getVmVendor());
        info.put("startTime", runtimeMXBean.getStartTime());
        info.put("uptime", runtimeMXBean.getUptime());
        
        // 内存信息
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        
        Map<String, Object> memoryInfo = new HashMap<>();
        memoryInfo.put("heapInit", heapMemoryUsage.getInit());
        memoryInfo.put("heapUsed", heapMemoryUsage.getUsed());
        memoryInfo.put("heapCommitted", heapMemoryUsage.getCommitted());
        memoryInfo.put("heapMax", heapMemoryUsage.getMax());
        
        memoryInfo.put("nonHeapInit", nonHeapMemoryUsage.getInit());
        memoryInfo.put("nonHeapUsed", nonHeapMemoryUsage.getUsed());
        memoryInfo.put("nonHeapCommitted", nonHeapMemoryUsage.getCommitted());
        memoryInfo.put("nonHeapMax", nonHeapMemoryUsage.getMax());
        
        info.put("memory", memoryInfo);
        
        // 系统属性
        info.put("osName", System.getProperty("os.name"));
        info.put("osVersion", System.getProperty("os.version"));
        info.put("osArch", System.getProperty("os.arch"));
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaHome", System.getProperty("java.home"));
        info.put("userDir", System.getProperty("user.dir"));
        
        return success(info);
    }
    
    /**
     * 健康检查
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public Result<String> healthCheck() {
        return success("服务运行正常");
    }
}