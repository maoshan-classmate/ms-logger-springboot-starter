package com.ms.annotation;


import com.ms.handler.MsLoggerHandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author maoshan-classmate
 * @date 2024-08-14
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE})
public @interface MsLogger {

    /**
     * 日志描述
     */
    String desc() default "";

    /**
     * 自定义日志处理
     * @return 自定义日志处理逻辑
     */
    Class<? extends MsLoggerHandler> handler() default MsLoggerHandler.class;

}
