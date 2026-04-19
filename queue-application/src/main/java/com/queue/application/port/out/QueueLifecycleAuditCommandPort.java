package com.queue.application.port.out;

import com.queue.application.dto.RecordQueueLifecycleAuditCommand;

public interface QueueLifecycleAuditCommandPort {
    boolean insertIfAbsent(RecordQueueLifecycleAuditCommand command);
}
