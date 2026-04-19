package com.queue.application.port.in;

public interface PromoteWaitingQueueUseCase {
    int promote(int requestedCount);
}