package com.queue.infrastructure.queue.redis.adapter;

import com.queue.application.port.out.QueueQueryPort;
import com.queue.infrastructure.queue.redis.support.RedisWaitingQueuePositionReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class RedisQueueQueryAdapter implements QueueQueryPort {

    private final RedisWaitingQueuePositionReader waitingQueuePositionReader;

    @Override
    public Long findRank(String queueId, String token) {
        return waitingQueuePositionReader.findPosition(queueId, token)
                .orElse(null);
    }
}
