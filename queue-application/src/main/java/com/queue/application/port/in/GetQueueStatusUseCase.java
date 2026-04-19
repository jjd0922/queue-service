package com.queue.application.port.in;

import com.queue.application.dto.GetQueueStatusQuery;
import com.queue.application.dto.QueueStatusResult;

public interface GetQueueStatusUseCase {
    QueueStatusResult getQueueStatus(GetQueueStatusQuery query);
}
