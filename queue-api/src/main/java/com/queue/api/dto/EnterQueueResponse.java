package com.queue.api.dto;

import com.queue.application.dto.EnterQueueResult;

import java.time.Instant;

public record EnterQueueResponse(
        String token,
        String queueId,
        Long userId,
        String status,
        Long position,
        Instant enteredAt,
        Instant expiresAt
) {
    public static EnterQueueResponse from(EnterQueueResult result) {
        return new EnterQueueResponse(
                result.token(),
                result.queueId(),
                result.userId(),
                result.status(),
                result.position(),
                result.enteredAt(),
                result.expiresAt()
        );
    }
}
