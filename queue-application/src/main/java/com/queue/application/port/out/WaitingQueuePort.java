package com.queue.application.port.out;

import java.util.Optional;

public interface WaitingQueuePort {
    void enqueue(String queueId, String token, long score);
    void remove(String queueId, String token);
    Optional<Long> findPosition(String queueId, String token);
    long count(String queueId);
}
