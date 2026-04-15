package com.queue.application.port.out;

import com.queue.application.dto.*;
import com.queue.domain.model.EnqueueDecision;

public interface QueueCommandPort {
    EnqueueDecision enqueueOrGetExisting(EnqueueCommand request);
    PromoteResult promoteWaitingEntries(PromoteCommand request);
    ExpireResult expireActiveEntries(ExpireCommand request);
}
