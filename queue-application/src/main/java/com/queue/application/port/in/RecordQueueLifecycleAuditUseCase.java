package com.queue.application.port.in;

import com.queue.application.dto.command.RecordQueueLifecycleAuditCommand;

public interface RecordQueueLifecycleAuditUseCase {
    boolean record(RecordQueueLifecycleAuditCommand command);
}
