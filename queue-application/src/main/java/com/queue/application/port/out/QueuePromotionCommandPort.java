package com.queue.application.port.out;

import com.queue.application.dto.command.PromoteCommand;
import com.queue.application.dto.result.PromoteResult;

public interface QueuePromotionCommandPort {
    PromoteResult promoteWaitingEntries(PromoteCommand command);
}
