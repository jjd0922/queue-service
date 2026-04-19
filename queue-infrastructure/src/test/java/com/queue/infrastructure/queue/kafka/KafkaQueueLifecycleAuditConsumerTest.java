package com.queue.infrastructure.queue.kafka;

import com.queue.application.dto.RecordQueueLifecycleAuditCommand;
import com.queue.application.port.in.RecordQueueLifecycleAuditUseCase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaQueueLifecycleAuditConsumerTest {

    private RecordQueueLifecycleAuditUseCase recordQueueLifecycleAuditUseCase;
    private QueueKafkaProperties queueKafkaProperties;
    private QueueLifecycleConsumerMetrics queueLifecycleConsumerMetrics;
    private QueueLifecycleAuditCommandMapper mapper;
    private KafkaQueueLifecycleAuditConsumer consumer;

    @BeforeEach
    void setUp() {
        recordQueueLifecycleAuditUseCase = mock(RecordQueueLifecycleAuditUseCase.class);
        queueKafkaProperties = new QueueKafkaProperties();
        queueKafkaProperties.setLifecycleConsumerGroup("queue-lifecycle-audit-v1");
        queueLifecycleConsumerMetrics = mock(QueueLifecycleConsumerMetrics.class);
        mapper = mock(QueueLifecycleAuditCommandMapper.class);
        consumer = new KafkaQueueLifecycleAuditConsumer(
                recordQueueLifecycleAuditUseCase,
                queueKafkaProperties,
                queueLifecycleConsumerMetrics,
                mapper
        );
    }

    @Test
    void consume_mapsMessageAndCallsUseCase() {
        QueueLifecycleEventMessage message = QueueLifecycleEventMessage.of(
                "event-1",
                "ENTERED",
                "token-1",
                1L,
                "WAITING",
                1L,
                Instant.parse("2026-04-18T01:00:00Z"),
                null
        );

        RecordQueueLifecycleAuditCommand command = new RecordQueueLifecycleAuditCommand(
                "event-1",
                "ENTERED",
                "token-1",
                1L,
                "WAITING",
                1L,
                Instant.parse("2026-04-18T01:00:00Z"),
                null,
                Instant.parse("2026-04-18T01:00:01Z")
        );

        ConsumerRecord<String, QueueLifecycleEventMessage> record = new ConsumerRecord<>(
                "queue.lifecycle.v1",
                0,
                10L,
                "token-1",
                message
        );
        Consumer<String, QueueLifecycleEventMessage> kafkaConsumer = mock(Consumer.class);
        when(kafkaConsumer.endOffsets(Set.of(new TopicPartition("queue.lifecycle.v1", 0))))
                .thenReturn(Map.of(new TopicPartition("queue.lifecycle.v1", 0), 20L));
        when(mapper.map(message)).thenReturn(command);
        when(recordQueueLifecycleAuditUseCase.record(command)).thenReturn(true);

        consumer.consume(record, kafkaConsumer);

        verify(mapper).map(message);
        verify(recordQueueLifecycleAuditUseCase).record(command);
        verify(queueLifecycleConsumerMetrics).incrementConsumed();
        verify(queueLifecycleConsumerMetrics).incrementSuccess();
    }
}
