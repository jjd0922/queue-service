package com.queue.infrastructure.queue.kafka;

import com.queue.application.port.in.PurgeQueueLifecycleAuditUseCase;
import com.queue.infrastructure.config.QueueAuditRetentionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueueLifecycleAuditRetentionWorkerTest {

    private PurgeQueueLifecycleAuditUseCase purgeQueueLifecycleAuditUseCase;
    private QueueAuditRetentionProperties queueAuditRetentionProperties;
    private QueueLifecycleAuditRetentionWorker worker;

    @BeforeEach
    void setUp() {
        purgeQueueLifecycleAuditUseCase = mock(PurgeQueueLifecycleAuditUseCase.class);
        queueAuditRetentionProperties = new QueueAuditRetentionProperties();

        Clock clock = Clock.fixed(Instant.parse("2026-04-19T00:00:00Z"), ZoneOffset.UTC);
        worker = new QueueLifecycleAuditRetentionWorker(
                purgeQueueLifecycleAuditUseCase,
                queueAuditRetentionProperties,
                clock
        );
    }

    @Test
    void execute_callsUseCase_whenEnabled() {
        queueAuditRetentionProperties.setEnabled(true);
        when(purgeQueueLifecycleAuditUseCase.purge(any())).thenReturn(1);

        worker.execute();

        verify(purgeQueueLifecycleAuditUseCase).purge(any());
    }

    @Test
    void execute_doesNothing_whenDisabled() {
        queueAuditRetentionProperties.setEnabled(false);

        worker.execute();

        verify(purgeQueueLifecycleAuditUseCase, never()).purge(any());
    }
}
