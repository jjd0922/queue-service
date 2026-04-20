package com.queue.application.port.in;

import com.queue.application.dto.command.PurgeQueueLifecycleAuditCommand;

public interface PurgeQueueLifecycleAuditUseCase {
    int purge(PurgeQueueLifecycleAuditCommand command);
}
