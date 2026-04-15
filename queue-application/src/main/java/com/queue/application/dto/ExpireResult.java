package com.queue.application.dto;

public record ExpireResult(
        String queueId,
        int requestedBatchSize,
        int actualExpiredCount
) {
}
