package com.queue.application.dto.command;

public record EnterQueueCommand(
        String queueId,
        Long userId
) {
    public EnterQueueCommand {
        if (queueId == null || queueId.isBlank()) {
            throw new IllegalArgumentException("queueId must not be blank");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
    }
}