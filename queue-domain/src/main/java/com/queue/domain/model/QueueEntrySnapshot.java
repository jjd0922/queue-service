package com.queue.domain.model;

import java.time.Instant;

public record QueueEntrySnapshot(
        String queueToken,
        QueueEntryStatus status,
        Instant enteredAt,
        Instant activatedAt,
        Instant expiresAt
) {
}
