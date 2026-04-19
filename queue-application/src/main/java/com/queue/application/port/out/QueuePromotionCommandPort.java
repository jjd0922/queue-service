package com.queue.application.port.out;

import com.queue.application.dto.PromoteCommand;
import com.queue.application.dto.PromoteResult;

public interface QueuePromotionCommandPort {
    PromoteResult promoteWaitingEntries(PromoteCommand command);
}
