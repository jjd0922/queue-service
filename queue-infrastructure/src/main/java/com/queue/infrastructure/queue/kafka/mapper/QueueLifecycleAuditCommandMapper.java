package com.queue.infrastructure.queue.kafka.mapper;

import com.queue.application.dto.RecordQueueLifecycleAuditCommand;
import com.queue.infrastructure.queue.kafka.model.QueueLifecycleEventMessage;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class QueueLifecycleAuditCommandMapper {

    private final Clock clock;

    public QueueLifecycleAuditCommandMapper(Clock clock) {
        this.clock = clock;
    }

    public RecordQueueLifecycleAuditCommand map(QueueLifecycleEventMessage message) {
        return new RecordQueueLifecycleAuditCommand(
                message.getEventId(),
                message.getEventType(),
                message.getQueueToken(),
                message.getUserId(),
                message.getStatus(),
                message.getSequence(),
                message.getOccurredAt(),
                message.getReason(),
                Instant.now(clock)
        );
    }
}
