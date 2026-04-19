package com.queue.application.port.out;

import com.queue.application.dto.ExpireCommand;
import com.queue.application.dto.ExpireResult;

import java.time.Instant;

public interface QueueExpirationCommandPort {
    ExpireResult expireActiveEntries(ExpireCommand request);
}
