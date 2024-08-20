package com.ms.pattern.strategy;

import com.ms.dto.MsLogger;
import org.aspectj.lang.JoinPoint;
import org.springframework.stereotype.Component;

/**
 * @author xuzw
 * @version 1.0
 * @description 简略日志策略
 * @date 2024/8/20 11:40
 */

@Component
public class SimpleLoggerStrategy extends MsLoggerAbstractStrategy {

    @Override
    public void doLog(MsLogger msLogger, JoinPoint joinPoint, String outParams, long cost) {
        LOGGER.info("\n\r=======================================\n\r" +
                        "日志描述:{} \n\r" +
                        "请求接口URL:{} \n\r" +
                        "处理耗时:{} ms \n\r" +
                        "=======================================\n\r",
                msLogger.getLogDesc(),
                msLogger.getApiUrl(),
                cost
        );
    }
}
