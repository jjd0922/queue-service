package com.queue.infrastructure.config;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        QueueActiveProperties.class
})
public class QueueRedisPropertiesConfig {
}
