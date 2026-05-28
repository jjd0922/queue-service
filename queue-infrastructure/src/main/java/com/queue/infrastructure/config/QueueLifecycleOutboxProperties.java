package com.queue.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "queue.lifecycle.outbox")
public class QueueLifecycleOutboxProperties {

    private boolean enabled = true;
    private long fixedDelayMs = 1000L;
    private int batchSize = 50;
    private int maxRetryCount = 5;
    private long retryBackoffMs = 5000L;
    private long publishTimeoutMs = 3000L;

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

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public long getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(long retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }

    public long getPublishTimeoutMs() {
        return publishTimeoutMs;
    }

    public void setPublishTimeoutMs(long publishTimeoutMs) {
        this.publishTimeoutMs = publishTimeoutMs;
    }
}
