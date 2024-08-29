package com.ms.handler;


import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author xuzw
 * @date 2024/8/20 14:25
 * @version 1.0
 */
public interface MsLoggerHandler {

    /**
     * 自定义日志处理
     * @param joinPoint 切点
     */
    void handleLogger(ProceedingJoinPoint joinPoint);


}
