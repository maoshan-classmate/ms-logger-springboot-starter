package com.ms.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author xuzw
 * @version 1.0
 * @description 日志策略枚举类
 * @date 2024/8/20 11:27
 */
@Getter
@AllArgsConstructor
public enum LoggerEnum {

    /**
     * 简单日志策略
     */
    SIMPLE("SIMPLE", "简单日志策略", "com.ms.pattern.strategy.SimpleLoggerStrategy"),

    /**
     * 详细日志策略
     */
    DETAIL("DETAIL", "详细日志策略", "com.ms.pattern.strategy.DetailLoggerStrategy");

    private final String strategyCode;
    private final String desc;
    private final String className;


}
