package com.ms.event;


import com.ms.handler.MsLoggerHandler;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author maoshan-classmates
 * @date 2024/8/29 22:37
 * @version 1.0
 */
@Component
public class MsLoggerListener {

    /**
     * 使用Spring event 事件异步处理日志增强器
     * @param msLoggerEvent 日志增强事件
     */
    @EventListener(MsLoggerEvent.class)
    @Async
    public void handleMsLoggerHandler(MsLoggerEvent msLoggerEvent) {
        MsLoggerHandler msLoggerHandler = msLoggerEvent.getMsLoggerHandler();
        ProceedingJoinPoint joinPoint = msLoggerEvent.getJoinPoint();
        msLoggerHandler.handleLogger(joinPoint);
    }
}
