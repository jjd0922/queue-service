package com.queue.infrastructure.queue.redis.adapter;

import com.queue.application.port.out.QueueEntryCommandPort;
import com.queue.application.port.out.QueueEntryQueryPort;
import com.queue.domain.model.QueueEntry;
import com.queue.infrastructure.queue.redis.key.RedisQueueKeyFactory;
import com.queue.infrastructure.queue.redis.mapper.RedisQueueEntryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisQueueEntryAdapter implements QueueEntryQueryPort, QueueEntryCommandPort {

    private final StringRedisTemplate redisTemplate;

    @Override
    public Optional<QueueEntry> findByToken(String queueId, String token) {
        String entryKey = RedisQueueKeyFactory.entryKey(queueId, token);
        Map<Object, Object> hash = redisTemplate.opsForHash().entries(entryKey);

        if (hash == null || hash.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(RedisQueueEntryMapper.fromHash(hash));
    }

    @Override
    public Optional<QueueEntry> findByQueueIdAndUserId(String queueId, Long userId) {
        String userKey = RedisQueueKeyFactory.userKey(queueId, userId);
        String token = redisTemplate.opsForValue().get(userKey);

        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        return findByToken(queueId, token);
    }

    @Override
    public void save(QueueEntry entry) {
        String entryKey = RedisQueueKeyFactory.entryKey(entry.getQueueId(), entry.getToken());
        String userKey = RedisQueueKeyFactory.userKey(entry.getQueueId(), entry.getUserId());

        redisTemplate.opsForHash().putAll(entryKey, RedisQueueEntryMapper.toHash(entry));
        redisTemplate.opsForValue().set(userKey, entry.getToken());
    }
}