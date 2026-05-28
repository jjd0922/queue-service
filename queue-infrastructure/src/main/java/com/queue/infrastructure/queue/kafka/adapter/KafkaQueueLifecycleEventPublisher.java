package com.queue.infrastructure.queue.kafka.adapter;

import com.queue.domain.event.QueueLifecycleEvent;
import com.queue.infrastructure.queue.kafka.config.QueueKafkaProperties;
import com.queue.infrastructure.queue.kafka.mapper.QueueLifecycleEventMessageMapper;
import com.queue.infrastructure.queue.kafka.model.QueueLifecycleEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaQueueLifecycleEventPublisher {

    private final KafkaTemplate<String, QueueLifecycleEventMessage> kafkaTemplate;
    private final QueueKafkaProperties queueKafkaProperties;
    private final QueueLifecycleEventMessageMapper mapper;

    public void publish(QueueLifecycleEvent event) {
        QueueLifecycleEventMessage message = mapper.map(event);

        kafkaTemplate.send(
                queueKafkaProperties.resolveLifecycleTopic(),
                event.getQueueToken(),
                message
        ).whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error(
                        "failed to publish queue lifecycle event. eventId={}, type={}, queueToken={}",
                        event.getEventId(),
                        event.getType(),
                        event.getQueueToken(),
                        throwable
                );
            }
        });
    }
}
