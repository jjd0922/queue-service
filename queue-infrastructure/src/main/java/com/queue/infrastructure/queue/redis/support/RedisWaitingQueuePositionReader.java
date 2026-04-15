package com.queue.infrastructure.queue.redis.support;

import com.queue.infrastructure.queue.redis.generator.RedisQueueKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisWaitingQueuePositionReader {

    private final StringRedisTemplate redisTemplate;
    private final RedisQueueKeyGenerator keyGenerator;

    public Optional<Long> findPosition(String queueId, String token) {
        Long rank = redisTemplate.opsForZSet()
                .rank(keyGenerator.waitingQueueKey(queueId), token);

        if (rank == null) {
            return Optional.empty();
        }

        return Optional.of(rank + 1);
    }
}
