package com.queue.application.dto;

public record GetQueueStatusQuery(
        String queueName,
        String queueToken
) {
}
