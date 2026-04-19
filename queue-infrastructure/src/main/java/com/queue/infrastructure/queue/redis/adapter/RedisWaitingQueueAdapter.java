package com.queue.infrastructure.queue.redis.adapter;

import com.queue.application.port.out.WaitingQueuePort;
import com.queue.infrastructure.queue.redis.key.RedisQueueKeyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisWaitingQueueAdapter implements WaitingQueuePort {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void enqueue(String queueId, String token, long score) {
        redisTemplate.opsForZSet()
                .add(RedisQueueKeyFactory.waitingKey(queueId), token, score);
    }

    @Override
    public void remove(String queueId, String token) {
        redisTemplate.opsForZSet()
                .remove(RedisQueueKeyFactory.waitingKey(queueId), token);
    }

    @Override
    public Optional<Long> findPosition(String queueId, String token) {
        Long rank = redisTemplate.opsForZSet()
                .rank(RedisQueueKeyFactory.waitingKey(queueId), token);

        if (rank == null) {
            return Optional.empty();
        }

        return Optional.of(rank + 1);
    }

    @Override
    public long count(String queueId) {
        Long size = redisTemplate.opsForZSet()
                .size(RedisQueueKeyFactory.waitingKey(queueId));

        return size == null ? 0L : size;
    }
}