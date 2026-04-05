package com.queue.infrastructure.queue.redis.adapter;

import com.queue.application.port.out.QueueQueryPort;
import com.queue.infrastructure.queue.redis.generator.RedisQueueKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class RedisQueueQueryAdapter implements QueueQueryPort {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisQueueKeyGenerator keyGenerator;

    @Override
    public Long findRank(String queueId, String token) {
        Long zeroBasedRank = stringRedisTemplate.opsForZSet()
                .rank(keyGenerator.waitingQueueKey(queueId), token);

        if (zeroBasedRank == null) {
            return null;
        }

        return zeroBasedRank + 1;
    }
}