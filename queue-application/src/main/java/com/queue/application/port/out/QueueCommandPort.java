package com.queue.application.port.out;

import com.queue.domain.queue.model.QueueEntry;

public interface QueueCommandPort {
    long nextSequence(String queueId);

    void saveWaiting(QueueEntry entry);

    void saveActive(QueueEntry entry);

    void saveTerminal(QueueEntry entry);

    void removeFromWaiting(String queueId, String token);

    void removeFromActive(String queueId, String token);
}
