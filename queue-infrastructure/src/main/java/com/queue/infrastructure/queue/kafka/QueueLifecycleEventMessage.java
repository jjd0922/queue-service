package com.queue.infrastructure.queue.kafka;

import lombok.Getter;

import java.time.Instant;

@Getter
public class QueueLifecycleEventMessage {

    private String eventId;
    private String eventType;
    private String queueToken;
    private Long userId;
    private String status;
    private Long sequence;
    private Instant occurredAt;
    private String reason;

    public static QueueLifecycleEventMessage of(
            String eventId,
            String eventType,
            String queueToken,
            Long userId,
            String status,
            Long sequence,
            Instant occurredAt,
            String reason
    ) {
        QueueLifecycleEventMessage message = new QueueLifecycleEventMessage();
        message.eventId = eventId;
        message.eventType = eventType;
        message.queueToken = queueToken;
        message.userId = userId;
        message.status = status;
        message.sequence = sequence;
        message.occurredAt = occurredAt;
        message.reason = reason;
        return message;
    }
}
