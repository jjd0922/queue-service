package com.queue.infrastructure.queue.kafka.consumer;

import com.queue.application.dto.command.RecordQueueLifecycleAuditCommand;
import com.queue.application.port.in.RecordQueueLifecycleAuditUseCase;
import com.queue.infrastructure.queue.kafka.config.QueueKafkaProperties;
import com.queue.infrastructure.queue.kafka.mapper.QueueLifecycleAuditCommandMapper;
import com.queue.infrastructure.queue.kafka.metrics.QueueLifecycleConsumerMetrics;
import com.queue.infrastructure.queue.kafka.model.QueueLifecycleEventMessage;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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

    @Test
    void consume_recordsZeroLagForLastRecordWithinSampleInterval() {
        queueKafkaProperties.setLagSampleIntervalMs(60_000L);

        QueueLifecycleEventMessage firstMessage = QueueLifecycleEventMessage.of(
                "event-1",
                "ENTERED",
                "token-1",
                1L,
                "WAITING",
                1L,
                Instant.parse("2026-04-18T01:00:00Z"),
                null
        );
        QueueLifecycleEventMessage lastMessage = QueueLifecycleEventMessage.of(
                "event-2",
                "ENTERED",
                "token-2",
                2L,
                "WAITING",
                2L,
                Instant.parse("2026-04-18T01:00:01Z"),
                null
        );
        RecordQueueLifecycleAuditCommand firstCommand = new RecordQueueLifecycleAuditCommand(
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
        RecordQueueLifecycleAuditCommand lastCommand = new RecordQueueLifecycleAuditCommand(
                "event-2",
                "ENTERED",
                "token-2",
                2L,
                "WAITING",
                2L,
                Instant.parse("2026-04-18T01:00:01Z"),
                null,
                Instant.parse("2026-04-18T01:00:02Z")
        );

        ConsumerRecord<String, QueueLifecycleEventMessage> firstRecord = new ConsumerRecord<>(
                "queue.lifecycle.v1",
                0,
                10L,
                "token-1",
                firstMessage
        );
        ConsumerRecord<String, QueueLifecycleEventMessage> lastRecord = new ConsumerRecord<>(
                "queue.lifecycle.v1",
                0,
                11L,
                "token-2",
                lastMessage
        );
        TopicPartition topicPartition = new TopicPartition("queue.lifecycle.v1", 0);
        Consumer<String, QueueLifecycleEventMessage> kafkaConsumer = mock(Consumer.class);
        when(kafkaConsumer.endOffsets(Set.of(topicPartition))).thenReturn(Map.of(topicPartition, 12L));
        when(mapper.map(firstMessage)).thenReturn(firstCommand);
        when(mapper.map(lastMessage)).thenReturn(lastCommand);
        when(recordQueueLifecycleAuditUseCase.record(firstCommand)).thenReturn(true);
        when(recordQueueLifecycleAuditUseCase.record(lastCommand)).thenReturn(true);

        consumer.consume(firstRecord, kafkaConsumer);
        consumer.consume(lastRecord, kafkaConsumer);

        verify(queueLifecycleConsumerMetrics).recordLag("queue.lifecycle.v1", 0, "queue-lifecycle-audit-v1", 1L);
        verify(queueLifecycleConsumerMetrics).recordLag("queue.lifecycle.v1", 0, "queue-lifecycle-audit-v1", 0L);
        verify(kafkaConsumer, times(1)).endOffsets(Set.of(topicPartition));
    }
}
