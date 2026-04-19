package com.queue.application.port.out;

import com.queue.domain.model.QueueEntry;

public interface QueueEntryCommandPort {
    void save(QueueEntry entry);
}