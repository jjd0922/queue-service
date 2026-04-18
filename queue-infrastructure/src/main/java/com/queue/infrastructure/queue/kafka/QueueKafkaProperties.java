package com.queue.infrastructure.queue.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "queue.kafka")
public class QueueKafkaProperties {

    private String lifecycleTopic;
    private String lifecycleDltTopic;
    private String lifecycleConsumerGroup = "queue-lifecycle-audit-v1";
    private int consumerMaxAttempts = 3;
    private long consumerBackoffMs = 1000;

    public String getLifecycleTopic() {
        return lifecycleTopic;
    }

    public void setLifecycleTopic(String lifecycleTopic) {
        this.lifecycleTopic = lifecycleTopic;
    }

    public String getLifecycleDltTopic() {
        return lifecycleDltTopic;
    }

    public void setLifecycleDltTopic(String lifecycleDltTopic) {
        this.lifecycleDltTopic = lifecycleDltTopic;
    }

    public String getLifecycleConsumerGroup() {
        return lifecycleConsumerGroup;
    }

    public void setLifecycleConsumerGroup(String lifecycleConsumerGroup) {
        this.lifecycleConsumerGroup = lifecycleConsumerGroup;
    }

    public int getConsumerMaxAttempts() {
        return consumerMaxAttempts;
    }

    public void setConsumerMaxAttempts(int consumerMaxAttempts) {
        this.consumerMaxAttempts = consumerMaxAttempts;
    }

    public long getConsumerBackoffMs() {
        return consumerBackoffMs;
    }

    public void setConsumerBackoffMs(long consumerBackoffMs) {
        this.consumerBackoffMs = consumerBackoffMs;
    }

    public String resolveLifecycleDltTopic() {
        if (lifecycleDltTopic == null || lifecycleDltTopic.isBlank()) {
            return lifecycleTopic + ".dlt";
        }
        return lifecycleDltTopic;
    }
}
