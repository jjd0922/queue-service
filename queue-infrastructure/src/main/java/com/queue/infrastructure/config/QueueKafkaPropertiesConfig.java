package com.queue.infrastructure.config;

import com.queue.infrastructure.queue.kafka.QueueKafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(QueueKafkaProperties.class)
public class QueueKafkaPropertiesConfig {
}
