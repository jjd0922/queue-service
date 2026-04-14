package com.queue.application.port.out;

import com.queue.application.dto.EnqueueCommand;
import com.queue.application.dto.PromoteCommand;
import com.queue.application.dto.PromoteResult;
import com.queue.domain.model.EnqueueDecision;

public interface QueueCommandPort {
    EnqueueDecision enqueueOrGetExisting(EnqueueCommand request);
    PromoteResult promoteWaitingEntries(PromoteCommand request);
}
