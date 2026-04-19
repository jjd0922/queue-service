package com.queue.application.dto;

import com.queue.domain.model.EnqueueDecision;

import java.time.Instant;

public record EnterQueueResult(
        String token,
        String queueId,
        Long userId,
        String status,
        String outcome,
        Long position,
        Instant enteredAt,
        Instant expiresAt
) {
    public static EnterQueueResult of(EnqueueDecision decision, Long position) {
        return new EnterQueueResult(
                decision.entry().getToken(),
                decision.entry().getQueueId(),
                decision.entry().getUserId(),
                decision.entry().getStatus().name(),
                decision.outcome().name(),
                position,
                decision.entry().getEnteredAt(),
                decision.entry().getExpiresAt()
        );
    }
}