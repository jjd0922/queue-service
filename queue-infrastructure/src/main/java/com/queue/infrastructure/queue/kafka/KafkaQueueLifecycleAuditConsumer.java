package com.queue.infrastructure.queue.kafka;

import com.queue.application.dto.RecordQueueLifecycleAuditCommand;
import com.queue.application.port.in.RecordQueueLifecycleAuditUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaQueueLifecycleAuditConsumer {

    private final RecordQueueLifecycleAuditUseCase recordQueueLifecycleAuditUseCase;
    private final QueueLifecycleAuditCommandMapper mapper;

    @KafkaListener(
            topics = "#{@queueKafkaProperties.lifecycleTopic}",
            groupId = "${queue.kafka.lifecycle-consumer-group:queue-lifecycle-audit-v1}"
    )
    public void consume(QueueLifecycleEventMessage message) {
        RecordQueueLifecycleAuditCommand command = mapper.map(message);
        boolean saved = recordQueueLifecycleAuditUseCase.record(command);

        if (!saved) {
            log.debug(
                    "queue lifecycle audit already exists. eventId={}, eventType={}",
                    command.eventId(),
                    command.eventType()
            );
        }
    }
}
