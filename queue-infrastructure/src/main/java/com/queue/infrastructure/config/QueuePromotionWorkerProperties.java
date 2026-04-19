package com.queue.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "queue.worker.promotion")
public class QueuePromotionWorkerProperties {

    private boolean enabled = true;
    private long fixedDelayMs = 1000L;
    private String queueId = "default";
    private int batchSize = 50;
    private int maxActiveCount = 100;
    private long activeTtlSeconds = 180L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxActiveCount() {
        return maxActiveCount;
    }

    public void setMaxActiveCount(int maxActiveCount) {
        this.maxActiveCount = maxActiveCount;
    }

    public long getActiveTtlSeconds() {
        return activeTtlSeconds;
    }

    public void setActiveTtlSeconds(long activeTtlSeconds) {
        this.activeTtlSeconds = activeTtlSeconds;
    }

    public Duration activeTtl() {
        return Duration.ofSeconds(activeTtlSeconds);
    }
}
