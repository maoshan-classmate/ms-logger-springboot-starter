package com.ms.config;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author xuzw
 * @date 2024/8/19 13:55
 * @version 1.0
 */

@Getter
@Setter
@ConfigurationProperties(prefix = "ms.logger")
public class MsLoggerProperties {

    /**
     * 是否开启日志打印
     */
    private boolean enable = true;

    /**
     * 日志监控路径
     */
    private String serverPath = "/**";


}
