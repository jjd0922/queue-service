package com.queue.application.port.out;

import com.queue.domain.queue.model.QueueEntry;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QueueQueryPort {
    Optional<QueueEntry> findByToken(String queueId, String token);

    Optional<QueueEntry> findByUserId(String queueId, Long userId);

    Optional<Long> findWaitingPosition(String queueId, String token);

    long countWaiting(String queueId);

    long countActive(String queueId);

    List<QueueEntry> findExpiredActiveEntries(String queueId, Instant now, int limit);
}
