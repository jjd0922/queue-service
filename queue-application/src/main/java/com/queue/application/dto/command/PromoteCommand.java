package com.queue.application.dto.command;

import java.time.Duration;
import java.time.Instant;

public record PromoteCommand(
        String queueId,
        Instant requestedAt,
        int maxActiveCount,
        int promoteBatchSize,
        Duration activeTtl
) {
}