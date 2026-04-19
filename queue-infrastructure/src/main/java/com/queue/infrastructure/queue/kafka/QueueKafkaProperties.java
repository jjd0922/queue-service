package com.queue.infrastructure.queue.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "queue.kafka")
public class QueueKafkaProperties {

    private String lifecycleTopic;

    public String getLifecycleTopic() {
        return lifecycleTopic;
    }

    public void setLifecycleTopic(String lifecycleTopic) {
        this.lifecycleTopic = lifecycleTopic;
    }
}
