package com.queue.application.port.out;

import com.queue.domain.model.QueueEntrySnapshot;

import java.util.Optional;

public interface QueueStatusQueryPort {
    Optional<QueueEntrySnapshot> findEntry(String queueToken);

    Optional<Long> findWaitingPosition(String queueName, String queueToken);

    boolean isActive(String queueName, String queueToken);
}
