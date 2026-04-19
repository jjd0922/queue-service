package com.queue.application.port.out;

import com.queue.application.dto.EnqueueRequest;
import com.queue.domain.model.EnqueueDecision;

public interface QueueCommandPort {

    EnqueueDecision enqueueOrGetExisting(EnqueueRequest request);
}
