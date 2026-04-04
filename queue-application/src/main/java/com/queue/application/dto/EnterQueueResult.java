package com.queue.application.dto;

import com.queue.domain.model.QueueEntry;

import java.time.Instant;

public record EnterQueueResult(
        String token,
        String queueId,
        Long userId,
        String status,
        Long position,
        Instant enteredAt,
        Instant expiresAt
) {
    public static EnterQueueResult of(QueueEntry entry, Long position) {
        return new EnterQueueResult(
                entry.getToken(),
                entry.getQueueId(),
                entry.getUserId(),
                entry.getStatus().name(),
                position,
                entry.getEnteredAt(),
                entry.getExpiresAt()
        );
    }
}