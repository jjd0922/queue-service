package com.queue.application.port.out;

import java.time.Instant;

public interface QueueLifecycleAuditManagementPort {
    int deleteOlderThan(Instant cutoff, int batchSize);
}
