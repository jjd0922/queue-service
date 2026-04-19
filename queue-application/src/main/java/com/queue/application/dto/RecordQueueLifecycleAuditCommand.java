package com.queue.application.dto;

import java.time.Instant;

public record RecordQueueLifecycleAuditCommand(
        String eventId,
        String eventType,
        String queueToken,
        Long userId,
        String status,
        Long sequence,
        Instant occurredAt,
        String reason,
        Instant receivedAt
) {
    public RecordQueueLifecycleAuditCommand {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }
        if (queueToken == null || queueToken.isBlank()) {
            throw new IllegalArgumentException("queueToken must not be blank");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be blank");
        }
        if (sequence == null || sequence <= 0) {
            throw new IllegalArgumentException("sequence must be positive");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }
        if (receivedAt == null) {
            throw new IllegalArgumentException("receivedAt must not be null");
        }
    }
}
