package com.queue.application.dto;

import java.time.Instant;

public record EnqueueCommand(
        String queueId,
        Long userId,
        Instant requestedAt
) {
}