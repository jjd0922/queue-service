package com.queue.domain.model;

public record EnqueueDecision(
        EnqueueOutcome outcome,
        QueueEntry entry
) {
    public boolean isNewlyCreated() {
        return outcome == EnqueueOutcome.CREATED;
    }
}
