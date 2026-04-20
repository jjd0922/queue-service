package com.queue.application.port.in;

import com.queue.application.dto.command.EnterQueueCommand;
import com.queue.application.dto.result.EnterQueueResult;

public interface EnterQueueUseCase {
    EnterQueueResult enter(EnterQueueCommand command);
}
