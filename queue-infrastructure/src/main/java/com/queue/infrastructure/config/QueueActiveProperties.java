package com.queue.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "queue.active")
public class QueueActiveProperties {

    private long tokenTtlSeconds = 1800L;

    public long getTokenTtlSeconds() {
        return tokenTtlSeconds;
    }

    public void setTokenTtlSeconds(long tokenTtlSeconds) {
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    public Duration tokenTtl() {
        return Duration.ofSeconds(tokenTtlSeconds);
    }
}
