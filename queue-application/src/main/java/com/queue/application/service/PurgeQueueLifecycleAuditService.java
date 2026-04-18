package com.queue.application.service;

import com.queue.application.dto.PurgeQueueLifecycleAuditCommand;
import com.queue.application.port.in.PurgeQueueLifecycleAuditUseCase;
import com.queue.application.port.out.QueueLifecycleAuditManagementPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class PurgeQueueLifecycleAuditService implements PurgeQueueLifecycleAuditUseCase {

    private final QueueLifecycleAuditManagementPort queueLifecycleAuditManagementPort;

    @Override
    public int purge(PurgeQueueLifecycleAuditCommand command) {
        Instant cutoff = command.requestedAt().minus(command.retentionDays(), ChronoUnit.DAYS);
        return queueLifecycleAuditManagementPort.deleteOlderThan(cutoff, command.batchSize());
    }
}
