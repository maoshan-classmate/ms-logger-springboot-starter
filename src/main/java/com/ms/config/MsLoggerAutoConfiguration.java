package com.ms.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author xuzw
 * @version 1.0
 * @date 2024/8/19 13:54
 */

@Configuration
@EnableConfigurationProperties(MsLoggerProperties.class)
public class MsLoggerAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "ms.logger", name = "enable", havingValue = "true")
    public MsLoggerProperties getMsLoggerProperties() {
        return new MsLoggerProperties();
    }

}
