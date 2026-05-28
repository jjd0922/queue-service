package com.queue.infrastructure.queue.kafka.outbox;

import java.time.Instant;

public record QueueLifecycleOutboxEvent(
        Long id,
        String eventId,
        String eventType,
        String queueToken,
        Long userId,
        String status,
        Long sequence,
        Instant occurredAt,
        String reason,
        String payload,
        int retryCount
) {
}
