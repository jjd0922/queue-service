package com.queue.application.service;

import com.queue.application.dto.command.RecordQueueLifecycleAuditCommand;
import com.queue.application.port.in.RecordQueueLifecycleAuditUseCase;
import com.queue.application.port.out.QueueLifecycleAuditCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RecordQueueLifecycleAuditService implements RecordQueueLifecycleAuditUseCase {

    private final QueueLifecycleAuditCommandPort queueLifecycleAuditCommandPort;

    @Override
    public boolean record(RecordQueueLifecycleAuditCommand command) {
        return queueLifecycleAuditCommandPort.insertIfAbsent(command);
    }
}
