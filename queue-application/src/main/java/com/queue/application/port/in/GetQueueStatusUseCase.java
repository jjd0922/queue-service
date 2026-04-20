package com.queue.application.port.in;

import com.queue.application.dto.query.GetQueueStatusQuery;
import com.queue.application.dto.result.QueueStatusResult;

public interface GetQueueStatusUseCase {
    QueueStatusResult getQueueStatus(GetQueueStatusQuery query);
}
