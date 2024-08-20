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

    SIMPLE("SIMPLE", "简单日志策略", "com.ms.pattern.strategy.SimpleLoggerStrategy"),
    DETAIL("DETAIL", "详细日志策略", "com.ms.pattern.strategy.DetailLoggerStrategy");

    private final String strategyCode;
    private final String desc;
    private final String className;


    /**
     * 获取日志策略类名
     * @param strategyCode 日志策略代码
     * @return 日志策略类名
     */
    public static String getLoggerStrategyClassName(String strategyCode) {
        for (LoggerEnum loggerEnum : LoggerEnum.values()) {
            if (loggerEnum.getStrategyCode().equals(strategyCode)) {
                return loggerEnum.getClassName();
            }
        }
        return SIMPLE.getClassName();
    }
}
