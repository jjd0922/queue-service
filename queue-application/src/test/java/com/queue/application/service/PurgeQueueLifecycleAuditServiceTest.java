package com.queue.application.service;

import com.queue.application.dto.PurgeQueueLifecycleAuditCommand;
import com.queue.application.port.out.QueueLifecycleAuditManagementPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurgeQueueLifecycleAuditServiceTest {

    private QueueLifecycleAuditManagementPort queueLifecycleAuditManagementPort;
    private PurgeQueueLifecycleAuditService service;

    @BeforeEach
    void setUp() {
        queueLifecycleAuditManagementPort = mock(QueueLifecycleAuditManagementPort.class);
        service = new PurgeQueueLifecycleAuditService(queueLifecycleAuditManagementPort);
    }

    @Test
    void purge_computesCutoffAndDelegates() {
        Instant requestedAt = Instant.parse("2026-04-19T00:00:00Z");
        PurgeQueueLifecycleAuditCommand command = new PurgeQueueLifecycleAuditCommand(30, 500, requestedAt);

        when(queueLifecycleAuditManagementPort.deleteOlderThan(Instant.parse("2026-03-20T00:00:00Z"), 500))
                .thenReturn(123);

        int purged = service.purge(command);

        assertThat(purged).isEqualTo(123);
        verify(queueLifecycleAuditManagementPort)
                .deleteOlderThan(Instant.parse("2026-03-20T00:00:00Z"), 500);
    }
}
