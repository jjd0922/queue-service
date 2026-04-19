package com.queue.application.port.in;

import com.queue.application.dto.EnterQueueCommand;
import com.queue.application.dto.EnterQueueResult;

public interface EnterQueueUseCase {
    EnterQueueResult enter(EnterQueueCommand command);
}
