package com.ms.pattern.factory;

import com.ms.enums.LoggerEnum;
import com.ms.pattern.strategy.MsLoggerAbstractStrategy;


/**
 * @author xuzw
 * @version 1.0
 * @description 日志策略工厂
 * @date 2024/8/20 13:20
 */
public class MsLoggerFactory {

    public static MsLoggerAbstractStrategy getMsLoggerStrategy(String strategyCode) {
        try {
            return (MsLoggerAbstractStrategy) Class.forName(LoggerEnum.getLoggerStrategyClassName(strategyCode)).getDeclaredConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
