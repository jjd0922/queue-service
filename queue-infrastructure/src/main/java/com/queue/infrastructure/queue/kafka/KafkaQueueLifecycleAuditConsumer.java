package com.queue.infrastructure.queue.kafka;

import com.queue.application.dto.RecordQueueLifecycleAuditCommand;
import com.queue.application.port.in.RecordQueueLifecycleAuditUseCase;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaQueueLifecycleAuditConsumer {

    private final RecordQueueLifecycleAuditUseCase recordQueueLifecycleAuditUseCase;
    private final QueueKafkaProperties queueKafkaProperties;
    private final QueueLifecycleConsumerMetrics queueLifecycleConsumerMetrics;
    private final QueueLifecycleAuditCommandMapper mapper;

    @KafkaListener(
            topics = "#{@queueKafkaProperties.lifecycleTopic}",
            groupId = "#{@queueKafkaProperties.lifecycleConsumerGroup}",
            containerFactory = "queueLifecycleKafkaListenerContainerFactory"
    )
    public void consume(
            @NonNull ConsumerRecord<String, QueueLifecycleEventMessage> record,
            @NonNull Consumer<String, QueueLifecycleEventMessage> consumer
    ) {
        long startedAtNanos = System.nanoTime();
        QueueLifecycleEventMessage message = record.value();
        if (message == null) {
            throw new IllegalArgumentException("queue lifecycle message must not be null");
        }
        String traceId = resolveTraceId(message);
        bindMdc(traceId, message.getEventId(), record);
        queueLifecycleConsumerMetrics.incrementConsumed();
        recordLag(record, consumer);

        try {
            RecordQueueLifecycleAuditCommand command = mapper.map(message);
            boolean saved = recordQueueLifecycleAuditUseCase.record(command);

            if (!saved) {
                queueLifecycleConsumerMetrics.incrementDuplicateIgnored();
                log.info(
                        "queue lifecycle consume duplicate ignored. eventType={}, queueToken={}, userId={}",
                        command.eventType(),
                        command.queueToken(),
                        command.userId()
                );
            } else {
                log.info(
                        "queue lifecycle consume success. eventType={}, queueToken={}, userId={}",
                        command.eventType(),
                        command.queueToken(),
                        command.userId()
                );
            }
            queueLifecycleConsumerMetrics.incrementSuccess();
        } catch (Exception e) {
            queueLifecycleConsumerMetrics.incrementFailure();
            log.error("queue lifecycle consume failed", e);
            throw e;
        } finally {
            queueLifecycleConsumerMetrics.recordProcessingLatencyNanos(System.nanoTime() - startedAtNanos);
            clearMdc();
        }
    }

    private void recordLag(
            ConsumerRecord<String, QueueLifecycleEventMessage> record,
            Consumer<String, QueueLifecycleEventMessage> consumer
    ) {
        try {
            TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(Set.of(topicPartition));
            long endOffset = endOffsets.getOrDefault(topicPartition, record.offset() + 1L);
            long lag = Math.max(endOffset - record.offset() - 1L, 0L);
            queueLifecycleConsumerMetrics.recordLag(
                    record.topic(),
                    record.partition(),
                    queueKafkaProperties.getLifecycleConsumerGroup(),
                    lag
            );
        } catch (Exception e) {
            log.debug("failed to compute consumer lag. topic={}, partition={}", record.topic(), record.partition(), e);
        }
    }

    private String resolveTraceId(QueueLifecycleEventMessage message) {
        if (message.getEventId() == null || message.getEventId().isBlank()) {
            return UUID.randomUUID().toString();
        }
        return message.getEventId();
    }

    private void bindMdc(String traceId, String eventId, ConsumerRecord<String, QueueLifecycleEventMessage> record) {
        MDC.put("traceId", traceId);
        MDC.put("eventId", eventId);
        MDC.put("kafkaTopic", record.topic());
        MDC.put("kafkaPartition", String.valueOf(record.partition()));
        MDC.put("kafkaOffset", String.valueOf(record.offset()));
    }

    private void clearMdc() {
        MDC.remove("traceId");
        MDC.remove("eventId");
        MDC.remove("kafkaTopic");
        MDC.remove("kafkaPartition");
        MDC.remove("kafkaOffset");
    }
}
