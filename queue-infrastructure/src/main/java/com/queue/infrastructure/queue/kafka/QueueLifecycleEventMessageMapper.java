package com.queue.infrastructure.queue.kafka;

import com.queue.domain.event.QueueLifecycleEvent;
import org.springframework.stereotype.Component;

@Component
public class QueueLifecycleEventMessageMapper {

    public QueueLifecycleEventMessage map(QueueLifecycleEvent event) {
        return QueueLifecycleEventMessage.of(
                event.getEventId(),
                event.getType().name(),
                event.getQueueToken(),
                event.getUserId(),
                event.getStatus().name(),
                event.getSequence(),
                event.getOccurredAt(),
                event.getReason()
        );
    }
}
