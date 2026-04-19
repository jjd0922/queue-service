package com.queue.application.dto;

public record ExpireAndPromoteResult(
        String queueId,
        int requestedExpireBatchSize,
        int actualExpiredCount,
        int requestedPromoteCount,
        int actualPromotedCount
) {
    public static ExpireAndPromoteResult of(
            String queueId,
            int requestedExpireBatchSize,
            int actualExpiredCount,
            int requestedPromoteCount,
            int actualPromotedCount
    ) {
        return new ExpireAndPromoteResult(
                queueId,
                requestedExpireBatchSize,
                actualExpiredCount,
                requestedPromoteCount,
                actualPromotedCount
        );
    }
}