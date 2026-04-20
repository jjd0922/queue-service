package com.queue.application.dto.query;

public record GetQueueStatusQuery(
        String queueName,
        String queueToken
) {
}
