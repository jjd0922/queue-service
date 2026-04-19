package com.queue.application.dto;

public record PromoteResult(
        String queueId,
        int requestedCount,
        int promotedCount
) {
    public static PromoteResult empty(String queueId, int requestedCount) {
        return new PromoteResult(queueId, requestedCount, 0);
    }

    public boolean hasPromoted() {
        return promotedCount > 0;
    }
}
