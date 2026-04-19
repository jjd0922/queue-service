package com.queue.application.port.in;

import com.queue.application.dto.RecordQueueLifecycleAuditCommand;

public interface RecordQueueLifecycleAuditUseCase {
    boolean record(RecordQueueLifecycleAuditCommand command);
}
