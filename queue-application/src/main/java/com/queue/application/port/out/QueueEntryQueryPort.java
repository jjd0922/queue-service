package com.queue.application.port.out;

import com.queue.domain.model.QueueEntry;

import java.util.Optional;

public interface QueueEntryQueryPort {
    Optional<QueueEntry> findByToken(String queueId, String token);
    Optional<QueueEntry> findByQueueIdAndUserId(String queueId, Long userId);
}