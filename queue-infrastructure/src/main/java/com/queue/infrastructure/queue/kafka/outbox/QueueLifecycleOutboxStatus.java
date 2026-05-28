package com.queue.infrastructure.queue.kafka.outbox;

public enum QueueLifecycleOutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED,
    DEAD
}
