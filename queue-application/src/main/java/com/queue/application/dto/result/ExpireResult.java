package com.queue.application.dto.result;

public record ExpireResult(
        String queueId,
        int requestedBatchSize,
        int actualExpiredCount
) {
}
