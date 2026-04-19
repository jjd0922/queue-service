package com.queue.application.port.in;

import com.queue.application.dto.PurgeQueueLifecycleAuditCommand;

public interface PurgeQueueLifecycleAuditUseCase {
    int purge(PurgeQueueLifecycleAuditCommand command);
}
