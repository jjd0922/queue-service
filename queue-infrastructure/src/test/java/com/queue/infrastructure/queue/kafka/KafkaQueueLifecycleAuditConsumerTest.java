package com.queue.infrastructure.queue.kafka;

import com.queue.application.dto.RecordQueueLifecycleAuditCommand;
import com.queue.application.port.in.RecordQueueLifecycleAuditUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaQueueLifecycleAuditConsumerTest {

    private RecordQueueLifecycleAuditUseCase recordQueueLifecycleAuditUseCase;
    private QueueLifecycleAuditCommandMapper mapper;
    private KafkaQueueLifecycleAuditConsumer consumer;

    @BeforeEach
    void setUp() {
        recordQueueLifecycleAuditUseCase = mock(RecordQueueLifecycleAuditUseCase.class);
        mapper = mock(QueueLifecycleAuditCommandMapper.class);
        consumer = new KafkaQueueLifecycleAuditConsumer(recordQueueLifecycleAuditUseCase, mapper);
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

        when(mapper.map(message)).thenReturn(command);
        when(recordQueueLifecycleAuditUseCase.record(command)).thenReturn(true);

        consumer.consume(message);

        verify(mapper).map(message);
        verify(recordQueueLifecycleAuditUseCase).record(command);
    }
}
