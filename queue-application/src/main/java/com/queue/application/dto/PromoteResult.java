package com.queue.application.dto;

import com.queue.domain.model.QueueEntry;

import java.util.List;

public record PromoteResult(
        String queueId,
        int requestedCount,
        List<QueueEntry> promotedEntries
) {
    public int promotedCount() {
        return promotedEntries.size();
    }
}
