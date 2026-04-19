package com.queue.application.dto;

public record ExpireActiveTokensResult(
        long expiredCount,
        int promotedCount
) {
    public static ExpireActiveTokensResult of(long expiredCount, int promotedCount) {
        return new ExpireActiveTokensResult(expiredCount, promotedCount);
    }
}