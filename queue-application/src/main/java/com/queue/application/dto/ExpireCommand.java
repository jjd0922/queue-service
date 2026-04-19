package com.queue.application.dto;

import java.time.Instant;

public record ExpireCommand(
        String queueId,
        Instant requestedAt,
        int expireBatchSize
) {
    public ExpireCommand {
        if (queueId == null || queueId.isBlank()) {
            throw new IllegalArgumentException("queueId must not be blank");
        }
        if (requestedAt == null) {
            throw new IllegalArgumentException("requestedAt must not be null");
        }
        if (expireBatchSize <= 0) {
            throw new IllegalArgumentException("expireBatchSize must be greater than zero");
        }
    }
}
