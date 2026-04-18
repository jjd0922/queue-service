package com.queue.infrastructure.queue.redis.mapper;

import com.queue.domain.model.QueueEntry;
import com.queue.domain.model.QueueEntryStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class RedisQueueEntryMapper {

    public QueueEntry from(String queueId, Map<Object, Object> hash) {
        if (hash == null || hash.isEmpty()) {
            throw new IllegalArgumentException("queue entry hash is empty");
        }

        return QueueEntry.restore(
                getString(hash, "token"),
                queueId,
                getLong(hash, "userId"),
                QueueEntryStatus.valueOf(getString(hash, "status")),
                getLong(hash, "sequence"),
                getInstant(hash, "enteredAt"),
                getInstantOrNull(hash, "activatedAt"),
                getInstantOrNull(hash, "expiresAt"),
                getInstant(hash, "lastUpdatedAt")
        );
    }

    private String getString(Map<Object, Object> hash, String key) {
        Object value = hash.get(key);
        if (value == null) {
            throw new IllegalArgumentException("missing redis field: " + key);
        }
        return value.toString();
    }

    private Long getLong(Map<Object, Object> hash, String key) {
        return Long.valueOf(getString(hash, key));
    }

    private Instant getInstant(Map<Object, Object> hash, String key) {
        return Instant.parse(getString(hash, key));
    }

    private Instant getInstantOrNull(Map<Object, Object> hash, String key) {
        Object value = hash.get(key);
        return value == null ? null : Instant.parse(value.toString());
    }
}
