package com.queue.infrastructure.queue.redis.adapter;

import com.queue.application.port.out.QueueStatusQueryPort;
import com.queue.domain.model.QueueEntrySnapshot;
import com.queue.domain.model.QueueEntryStatus;
import com.queue.infrastructure.queue.redis.generator.RedisQueueKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisQueueStatusQueryAdapter implements QueueStatusQueryPort {

    private final StringRedisTemplate redisTemplate;
    private final RedisQueueKeyGenerator keyGenerator;

    @Override
    public Optional<QueueEntrySnapshot> findEntry(String token) {
        String entryKey = keyGenerator.entryKey(token);
        Map<Object, Object> hash = redisTemplate.opsForHash().entries(entryKey);

        if (hash.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new QueueEntrySnapshot(
                token,
                parseStatus((String) hash.get("status")),
                parseInstant((String) hash.get("enteredAt")),
                parseInstant((String) hash.get("activatedAt")),
                parseInstant((String) hash.get("expiresAt"))
        ));
    }

    @Override
    public Optional<Long> findWaitingRank(String queueId, String token) {
        Long rank = redisTemplate.opsForZSet()
                .rank(keyGenerator.waitingQueueKey(queueId), token);
        return Optional.ofNullable(rank);
    }

    @Override
    public boolean isActive(String queueId, String token) {
        Double score = redisTemplate.opsForZSet()
                .score(keyGenerator.activeQueueKey(queueId), token);
        return score != null;
    }

    private QueueEntryStatus parseStatus(String value) {
        return value == null ? null : QueueEntryStatus.valueOf(value);
    }

    private Instant parseInstant(String value) {
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }
}
