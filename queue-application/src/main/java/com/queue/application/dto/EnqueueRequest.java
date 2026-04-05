package com.queue.application.dto;

import java.time.Instant;

public record EnqueueRequest(
        String queueId,
        Long userId,
        Instant requestedAt
) {
}