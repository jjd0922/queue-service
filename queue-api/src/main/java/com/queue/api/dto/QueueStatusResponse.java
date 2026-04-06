package com.queue.api.dto;

import com.queue.application.dto.QueueStatusResult;

import java.time.Instant;

public record QueueStatusResponse(
        String queueName,
        String queueToken,
        String status,
        Long position,
        Long aheadCount,
        Instant enteredAt,
        Instant activatedAt,
        Instant expiresAt
) {
    public static QueueStatusResponse from(QueueStatusResult result) {
        return new QueueStatusResponse(
                result.queueName(),
                result.queueToken(),
                result.status(),
                result.position(),
                result.aheadCount(),
                result.enteredAt(),
                result.activatedAt(),
                result.expiresAt()
        );
    }
}
