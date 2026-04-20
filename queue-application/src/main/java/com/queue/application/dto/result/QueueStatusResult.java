package com.queue.application.dto.result;

import java.time.Instant;

public record QueueStatusResult(
        String queueName,
        String queueToken,
        String status,
        Long position,
        Long aheadCount,
        Instant enteredAt,
        Instant activatedAt,
        Instant expiresAt
) {
}
