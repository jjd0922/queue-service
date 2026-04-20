package com.queue.infrastructure.queue.kafka.consumer;

import com.queue.application.dto.command.RecordQueueLifecycleAuditCommand;
import com.queue.application.port.in.RecordQueueLifecycleAuditUseCase;
import com.queue.infrastructure.queue.kafka.config.QueueKafkaProperties;
import com.queue.infrastructure.queue.kafka.mapper.QueueLifecycleAuditCommandMapper;
import com.queue.infrastructure.queue.kafka.metrics.QueueLifecycleConsumerMetrics;
import com.queue.infrastructure.queue.kafka.model.QueueLifecycleEventMessage;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaQueueLifecycleAuditConsumer {

    private final RecordQueueLifecycleAuditUseCase recordQueueLifecycleAuditUseCase;
    private final QueueKafkaProperties queueKafkaProperties;
    private final QueueLifecycleConsumerMetrics queueLifecycleConsumerMetrics;
    private final QueueLifecycleAuditCommandMapper mapper;
    private final ConcurrentMap<TopicPartition, Long> lagSampledAtByPartition = new ConcurrentHashMap<>();

    @KafkaListener(
            topics = "${queue.kafka.lifecycle-topic:queue.lifecycle.v1}",
            groupId = "${queue.kafka.lifecycle-consumer-group:queue-lifecycle-audit-v1}",
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
            if (!shouldSampleLag(topicPartition)) {
                return;
            }
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

    private boolean shouldSampleLag(TopicPartition topicPartition) {
        long now = System.currentTimeMillis();
        long intervalMs = Math.max(queueKafkaProperties.getLagSampleIntervalMs(), 0L);
        Long lastSampledAt = lagSampledAtByPartition.get(topicPartition);
        if (lastSampledAt == null || now - lastSampledAt >= intervalMs) {
            lagSampledAtByPartition.put(topicPartition, now);
            return true;
        }
        return false;
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
