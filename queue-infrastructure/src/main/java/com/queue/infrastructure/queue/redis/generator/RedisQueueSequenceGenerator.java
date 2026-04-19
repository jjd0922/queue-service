package com.queue.infrastructure.queue.redis.generator;

import com.queue.application.port.out.QueueSequenceGenerator;
import com.queue.infrastructure.queue.redis.key.RedisQueueKeyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisQueueSequenceGenerator implements QueueSequenceGenerator {

    private final StringRedisTemplate redisTemplate;

    @Override
    public Long nextSequence(String queueId) {
        Long next = redisTemplate.opsForValue()
                .increment(RedisQueueKeyFactory.sequenceKey(queueId));

        if (next == null || next <= 0) {
            throw new IllegalStateException("failed to generate queue sequence");
        }

        return next;
    }
}