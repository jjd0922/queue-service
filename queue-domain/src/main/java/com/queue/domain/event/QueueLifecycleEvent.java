package com.queue.domain.event;

import com.queue.domain.model.QueueEntryStatus;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class QueueLifecycleEvent {

    private final String eventId;
    private final QueueLifecycleEventType type;
    private final String queueToken;
    private final Long userId;
    private final QueueEntryStatus status;
    private final Long sequence;
    private final Instant occurredAt;
    private final String reason;

    private QueueLifecycleEvent(
            String eventId,
            QueueLifecycleEventType type,
            String queueToken,
            Long userId,
            QueueEntryStatus status,
            Long sequence,
            Instant occurredAt,
            String reason
    ) {
        this.eventId = eventId;
        this.type = type;
        this.queueToken = queueToken;
        this.userId = userId;
        this.status = status;
        this.sequence = sequence;
        this.occurredAt = occurredAt;
        this.reason = reason;
    }

    public static QueueLifecycleEvent entered(
            String queueToken,
            Long userId,
            QueueEntryStatus status,
            Long sequence,
            Instant occurredAt
    ) {
        return new QueueLifecycleEvent(
                UUID.randomUUID().toString(),
                QueueLifecycleEventType.ENTERED,
                queueToken,
                userId,
                status,
                sequence,
                occurredAt,
                null
        );
    }

    public static QueueLifecycleEvent admitted(
            String queueToken,
            Long userId,
            QueueEntryStatus status,
            Long sequence,
            Instant occurredAt
    ) {
        return new QueueLifecycleEvent(
                UUID.randomUUID().toString(),
                QueueLifecycleEventType.ADMITTED,
                queueToken,
                userId,
                status,
                sequence,
                occurredAt,
                null
        );
    }

    public static QueueLifecycleEvent expired(
            String queueToken,
            Long userId,
            QueueEntryStatus status,
            Long sequence,
            Instant occurredAt,
            String reason
    ) {
        return new QueueLifecycleEvent(
                UUID.randomUUID().toString(),
                QueueLifecycleEventType.EXPIRED,
                queueToken,
                userId,
                status,
                sequence,
                occurredAt,
                reason
        );
    }
}
