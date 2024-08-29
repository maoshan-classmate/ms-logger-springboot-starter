package com.ms.event;

import com.ms.handler.MsLoggerHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author maoshan-classmates
 * @version 1.0
 * @date 2024/8/29 22:37
 */
@Data
@AllArgsConstructor
public class MsLoggerEvent {

    /**
     * 自定义日志处理器
     */
    private MsLoggerHandler msLoggerHandler;

    /**
     * 切点
     */
    private ProceedingJoinPoint joinPoint;

}
