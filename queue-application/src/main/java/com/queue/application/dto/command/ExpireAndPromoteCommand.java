package com.queue.application.dto.command;

import java.time.Duration;
import java.time.Instant;

public record ExpireAndPromoteCommand(
        String queueId,
        Instant requestedAt,
        int expireBatchSize,
        int promoteBatchSize,
        int maxActiveCount,
        Duration activeTtl
) {
    public ExpireAndPromoteCommand {
        if (queueId == null || queueId.isBlank()) {
            throw new IllegalArgumentException("queueId must not be blank");
        }
        if (requestedAt == null) {
            throw new IllegalArgumentException("requestedAt must not be null");
        }
        if (expireBatchSize <= 0) {
            throw new IllegalArgumentException("expireBatchSize must be greater than zero");
        }
        if (promoteBatchSize <= 0) {
            throw new IllegalArgumentException("promoteBatchSize must be greater than zero");
        }
        if (maxActiveCount <= 0) {
            throw new IllegalArgumentException("maxActiveCount must be greater than zero");
        }
        if (activeTtl == null || activeTtl.isNegative() || activeTtl.isZero()) {
            throw new IllegalArgumentException("activeTtl must be greater than zero");
        }
    }
}
