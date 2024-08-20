package com.ms.config;

import com.ms.enums.LoggerEnum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author xuzw
 * @version 1.0
 * @date 2024/8/19 13:55
 */

@Getter
@Setter
@ConfigurationProperties(prefix = "ms.logger")
public class MsLoggerProperties {

    private static MsLoggerProperties instance;

    public static MsLoggerProperties getInstance() {
        if (instance == null) {
            instance = new MsLoggerProperties();
        }
        return instance;
    }

    /**
     * 是否开启日志打印
     */
    private boolean enable = true;

    /**
     * 日志打印策略
     */
    private LoggerEnum loggerStrategy = LoggerEnum.SIMPLE;


}
