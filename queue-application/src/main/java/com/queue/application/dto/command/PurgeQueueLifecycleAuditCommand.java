package com.queue.application.dto.command;

import java.time.Instant;

public record PurgeQueueLifecycleAuditCommand(
        int retentionDays,
        int batchSize,
        Instant requestedAt
) {
    public PurgeQueueLifecycleAuditCommand {
        if (retentionDays <= 0) {
            throw new IllegalArgumentException("retentionDays must be positive");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        if (requestedAt == null) {
            throw new IllegalArgumentException("requestedAt must not be null");
        }
    }
}
