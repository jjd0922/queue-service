package com.queue.application.port.out;

import com.queue.application.dto.command.EnqueueCommand;
import com.queue.domain.model.EnqueueDecision;

public interface QueueEnqueueCommandPort {
    EnqueueDecision enqueueOrGetExisting(EnqueueCommand request);
}
