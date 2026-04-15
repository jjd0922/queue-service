package com.queue.application.port.out;

import java.time.Instant;

public interface QueueExpirationCommandPort {
    long expireActiveTokens(Instant now, int batchSize);
}
