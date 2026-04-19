package com.queue.domain.model;

public record EnqueueDecision(
        EnqueueOutcome outcome,
        QueueEntry entry
) {
}
