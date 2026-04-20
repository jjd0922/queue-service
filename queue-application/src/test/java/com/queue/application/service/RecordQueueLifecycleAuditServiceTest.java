package com.queue.application.service;

import com.queue.application.dto.command.RecordQueueLifecycleAuditCommand;
import com.queue.application.port.out.QueueLifecycleAuditCommandPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordQueueLifecycleAuditServiceTest {

    private QueueLifecycleAuditCommandPort queueLifecycleAuditCommandPort;
    private RecordQueueLifecycleAuditService service;

    @BeforeEach
    void setUp() {
        queueLifecycleAuditCommandPort = mock(QueueLifecycleAuditCommandPort.class);
        service = new RecordQueueLifecycleAuditService(queueLifecycleAuditCommandPort);
    }

    @Test
    void record_delegatesToPortAndReturnsResult() {
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

        when(queueLifecycleAuditCommandPort.insertIfAbsent(command)).thenReturn(true);

        boolean saved = service.record(command);

        assertThat(saved).isTrue();
        verify(queueLifecycleAuditCommandPort).insertIfAbsent(command);
    }
}
