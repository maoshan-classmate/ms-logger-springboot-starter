package com.ms.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author maoshan-classmate
 * @date 2024-08-14
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MsLogger {

    /**
     * 日志描述
     */
    String desc() default "";


}
