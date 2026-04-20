package com.queue.infrastructure.queue.kafka.mapper;

import com.queue.application.dto.RecordQueueLifecycleAuditCommand;
import com.queue.infrastructure.queue.kafka.model.QueueLifecycleEventMessage;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class QueueLifecycleAuditCommandMapperTest {

    @Test
    void map_buildsCommandWithReceivedAt() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-18T02:00:00Z"), ZoneOffset.UTC);
        QueueLifecycleAuditCommandMapper mapper = new QueueLifecycleAuditCommandMapper(clock);

        QueueLifecycleEventMessage message = QueueLifecycleEventMessage.of(
                "event-1",
                "ADMITTED",
                "token-1",
                7L,
                "ACTIVE",
                10L,
                Instant.parse("2026-04-18T01:59:59Z"),
                null
        );

        RecordQueueLifecycleAuditCommand command = mapper.map(message);

        assertThat(command.eventId()).isEqualTo("event-1");
        assertThat(command.eventType()).isEqualTo("ADMITTED");
        assertThat(command.queueToken()).isEqualTo("token-1");
        assertThat(command.userId()).isEqualTo(7L);
        assertThat(command.status()).isEqualTo("ACTIVE");
        assertThat(command.sequence()).isEqualTo(10L);
        assertThat(command.occurredAt()).isEqualTo(Instant.parse("2026-04-18T01:59:59Z"));
        assertThat(command.receivedAt()).isEqualTo(Instant.parse("2026-04-18T02:00:00Z"));
    }
}
