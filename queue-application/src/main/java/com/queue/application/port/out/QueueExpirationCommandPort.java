package com.queue.application.port.out;

import com.queue.application.dto.command.ExpireCommand;
import com.queue.application.dto.result.ExpireResult;

public interface QueueExpirationCommandPort {
    ExpireResult expireActiveEntries(ExpireCommand request);
}
