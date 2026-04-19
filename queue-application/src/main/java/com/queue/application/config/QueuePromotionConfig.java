package com.queue.application.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationPropertiesScan(basePackageClasses = QueuePromotionProperties.class)
public class QueuePromotionConfig {
}