package com.queue.application.port.out;

public interface QueueSequenceGenerator {
    Long nextSequence(String queueId);
}
