package com.queue.infrastructure.queue.redis;

import com.queue.application.port.out.QueueCommandPort;
import com.queue.application.port.out.QueueQueryPort;
import com.queue.domain.queue.model.QueueEntry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class RedisQueueAdapter implements QueueCommandPort, QueueQueryPort {

    private static final Duration TERMINAL_ENTRY_TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;
    private final RedisQueueEntryMapper mapper;

    public RedisQueueAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.mapper = new RedisQueueEntryMapper();
    }

    @Override
    public Optional<QueueEntry> findByToken(String queueId, String token) {
        String entryKey = RedisQueueKeyFactory.entryKey(queueId, token);
        Map<Object, Object> hash = redisTemplate.opsForHash().entries(entryKey);

        if (hash == null || hash.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mapper.fromHash(hash));
    }

    @Override
    public Optional<QueueEntry> findByUserId(String queueId, Long userId) {
        String userKey = RedisQueueKeyFactory.userKey(queueId, userId);
        String token = redisTemplate.opsForValue().get(userKey);

        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        return findByToken(queueId, token);
    }

    @Override
    public long nextSequence(String queueId) {
        Long next = redisTemplate.opsForValue().increment(RedisQueueKeyFactory.sequenceKey(queueId));
        if (next == null) {
            throw new IllegalStateException("failed to generate queue sequence");
        }
        return next;
    }

    @Override
    public void saveWaiting(QueueEntry entry) {
        String entryKey = RedisQueueKeyFactory.entryKey(entry.getQueueId(), entry.getToken());
        String waitingKey = RedisQueueKeyFactory.waitingKey(entry.getQueueId());
        String userKey = RedisQueueKeyFactory.userKey(entry.getQueueId(), entry.getUserId());

        redisTemplate.opsForHash().putAll(entryKey, mapper.toHash(entry));
        redisTemplate.opsForZSet().add(waitingKey, entry.getToken(), entry.getSequence());
        redisTemplate.opsForValue().set(userKey, entry.getToken());
    }

    @Override
    public void saveActive(QueueEntry entry) {
        if (entry.getExpiresAt() == null) {
            throw new IllegalArgumentException("active entry must have expiresAt");
        }

        String entryKey = RedisQueueKeyFactory.entryKey(entry.getQueueId(), entry.getToken());
        String waitingKey = RedisQueueKeyFactory.waitingKey(entry.getQueueId());
        String activeKey = RedisQueueKeyFactory.activeKey(entry.getQueueId());
        String userKey = RedisQueueKeyFactory.userKey(entry.getQueueId(), entry.getUserId());

        redisTemplate.opsForHash().putAll(entryKey, mapper.toHash(entry));
        redisTemplate.opsForZSet().remove(waitingKey, entry.getToken());
        redisTemplate.opsForZSet().add(activeKey, entry.getToken(), entry.getExpiresAt().toEpochMilli());
        redisTemplate.opsForValue().set(userKey, entry.getToken());
    }

    @Override
    public void saveTerminal(QueueEntry entry) {
        String entryKey = RedisQueueKeyFactory.entryKey(entry.getQueueId(), entry.getToken());
        String waitingKey = RedisQueueKeyFactory.waitingKey(entry.getQueueId());
        String activeKey = RedisQueueKeyFactory.activeKey(entry.getQueueId());
        String userKey = RedisQueueKeyFactory.userKey(entry.getQueueId(), entry.getUserId());

        redisTemplate.opsForHash().putAll(entryKey, mapper.toHash(entry));
        redisTemplate.opsForZSet().remove(waitingKey, entry.getToken());
        redisTemplate.opsForZSet().remove(activeKey, entry.getToken());
        redisTemplate.delete(userKey);
        redisTemplate.expire(entryKey, TERMINAL_ENTRY_TTL);
    }

    @Override
    public void removeFromWaiting(String queueId, String token) {
        redisTemplate.opsForZSet().remove(RedisQueueKeyFactory.waitingKey(queueId), token);
    }

    @Override
    public void removeFromActive(String queueId, String token) {
        redisTemplate.opsForZSet().remove(RedisQueueKeyFactory.activeKey(queueId), token);
    }

    @Override
    public Optional<Long> findWaitingPosition(String queueId, String token) {
        Long rank = redisTemplate.opsForZSet().rank(RedisQueueKeyFactory.waitingKey(queueId), token);
        if (rank == null) {
            return Optional.empty();
        }
        return Optional.of(rank + 1);
    }

    @Override
    public long countWaiting(String queueId) {
        Long size = redisTemplate.opsForZSet().size(RedisQueueKeyFactory.waitingKey(queueId));
        return size == null ? 0L : size;
    }

    @Override
    public long countActive(String queueId) {
        Long size = redisTemplate.opsForZSet().size(RedisQueueKeyFactory.activeKey(queueId));
        return size == null ? 0L : size;
    }

    @Override
    public List<QueueEntry> findExpiredActiveEntries(String queueId, Instant now, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        String activeKey = RedisQueueKeyFactory.activeKey(queueId);
        Set<String> tokens = redisTemplate.opsForZSet()
                .rangeByScore(activeKey, 0, now.toEpochMilli(), 0, limit);

        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }

        List<QueueEntry> result = new ArrayList<>();
        for (String token : tokens) {
            findByToken(queueId, token).ifPresent(result::add);
        }
        return result;
    }
}
