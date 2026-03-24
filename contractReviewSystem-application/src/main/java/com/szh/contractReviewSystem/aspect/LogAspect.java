package com.szh.contractReviewSystem.aspect;

import com.szh.contractReviewSystem.utils.HttpUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * 日志切面类 */
@Aspect
@Component
public class LogAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);
    
    /**
     * 配置织入点
     */
    @Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void logPointCut() {
    }
    
    /**
     * 前置通知
     */
    @Before("logPointCut()")
    public void doBefore(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        
        HttpServletRequest request = attributes.getRequest();
        String clientIp = HttpUtils.getClientIp(request);
        String requestUrl = HttpUtils.getRequestUrl(request);
        String method = request.getMethod();
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        logger.info("==========================================");
        logger.info("请求地址: {}", requestUrl);
        logger.info("请求方式: {}", method);
        logger.info("请求IP: {}", clientIp);
        logger.info("类名方法: {}.{}", className, methodName);
        logger.info("请求参数: {}", Arrays.toString(args));
        logger.info("==========================================");
    }
    
    /**
     * 后置通知
     */
    @After("logPointCut()")
    public void doAfter(JoinPoint joinPoint) {
        logger.info("请求处理完成");
    }
    
    /**
     * 环绕通知
     */
    @Around("logPointCut()")
    public Object doAround(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = point.proceed();
        long endTime = System.currentTimeMillis();
        
        logger.info("耗时: {} ms", (endTime - startTime));
        logger.info("返回结果: {}", result);
        logger.info("==========================================");
        
        return result;
    }
    
    /**
     * 异常通知
     */
    @AfterThrowing(pointcut = "logPointCut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        logger.error("发生异常: {}", e.getMessage(), e);
    }
}