package com.ms.pattern.strategy;

import cn.hutool.json.JSONUtil;
import com.ms.dto.MsLogger;
import org.aspectj.lang.JoinPoint;
import org.springframework.stereotype.Component;

/**
 * @description 详细日志策略
 * @author xuzw
 * @date 2024/8/20 11:41
 * @version 1.0
 */

@Component
public class DetailLoggerStrategy extends MsLoggerAbstractStrategy{

    @Override
    public void doLog(MsLogger msLogger, JoinPoint joinPoint, String outParams, long cost) {
        LOGGER.info("\n\r=======================================\n\r" +
                        "日志描述:{} \n\r" +
                        "请求地址:{} \n\r" +
                        "请求接口URL:{} \n\r"+
                        "请求方式:{} \n\r" +
                        "请求类方法:{} \n\r" +
                        "请求方法参数:{} \n\r" +
                        "返回报文:{} \n\r" +
                        "处理耗时:{} ms \n\r" +
                        "=======================================\n\r",
                msLogger.getLogDesc(),
                msLogger.getIpAddress(),
                msLogger.getApiUrl(),
                msLogger.getMethodName(),
                joinPoint.getSignature(),
                JSONUtil.toJsonStr(filterArgs(joinPoint.getArgs())),
                outParams,
                cost
        );
    }
}
