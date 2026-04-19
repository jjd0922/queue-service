package com.queue.application.port.out;

public interface QueueQueryPort {
    Long findRank(String queueId, String token);
}
