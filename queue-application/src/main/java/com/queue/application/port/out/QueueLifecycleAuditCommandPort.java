package com.queue.application.port.out;

import com.queue.application.dto.command.RecordQueueLifecycleAuditCommand;

public interface QueueLifecycleAuditCommandPort {
    boolean insertIfAbsent(RecordQueueLifecycleAuditCommand command);
}
