package com.ms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

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
     * 日志监控路径
     */
    private List<String> includePaths;


}
