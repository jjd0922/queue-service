package com.queue.infrastructure.queue.redis.mapper;

import com.queue.domain.model.QueueEntry;
import com.queue.domain.model.QueueStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class RedisQueueEntryMapper {

    private static final String TOKEN = "token";
    private static final String QUEUE_ID = "queueId";
    private static final String USER_ID = "userId";
    private static final String STATUS = "status";
    private static final String SEQUENCE = "sequence";
    private static final String ENTERED_AT = "enteredAt";
    private static final String ACTIVATED_AT = "activatedAt";
    private static final String EXPIRES_AT = "expiresAt";
    private static final String LAST_UPDATED_AT = "lastUpdatedAt";

    public Map<String, String> toHash(QueueEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");

        Map<String, String> hash = new LinkedHashMap<>();
        hash.put(TOKEN, entry.getToken());
        hash.put(QUEUE_ID, entry.getQueueId());
        hash.put(USER_ID, String.valueOf(entry.getUserId()));
        hash.put(STATUS, entry.getStatus().name());
        hash.put(SEQUENCE, String.valueOf(entry.getSequence()));
        hash.put(ENTERED_AT, String.valueOf(toEpochMilli(entry.getEnteredAt())));
        putIfNotNull(hash, ACTIVATED_AT, entry.getActivatedAt());
        putIfNotNull(hash, EXPIRES_AT, entry.getExpiresAt());
        hash.put(LAST_UPDATED_AT, String.valueOf(toEpochMilli(entry.getLastUpdatedAt())));
        return hash;
    }

    public QueueEntry fromHash(Map<Object, Object> source) {
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("source hash is empty");
        }

        String token = requiredText(source, TOKEN);
        String queueId = requiredText(source, QUEUE_ID);
        Long userId = requiredLong(source, USER_ID);
        QueueStatus status = QueueStatus.valueOf(requiredText(source, STATUS));
        Long sequence = requiredLong(source, SEQUENCE);
        Instant enteredAt = requiredInstant(source, ENTERED_AT);
        Instant activatedAt = optionalInstant(source, ACTIVATED_AT);
        Instant expiresAt = optionalInstant(source, EXPIRES_AT);
        Instant lastUpdatedAt = requiredInstant(source, LAST_UPDATED_AT);

        return QueueEntry.restore(
                token,
                queueId,
                userId,
                status,
                sequence,
                enteredAt,
                activatedAt,
                expiresAt,
                lastUpdatedAt
        );
    }

    private void putIfNotNull(Map<String, String> hash, String key, Instant value) {
        if (value != null) {
            hash.put(key, String.valueOf(toEpochMilli(value)));
        }
    }

    private static long toEpochMilli(Instant instant) {
        return instant.toEpochMilli();
    }

    private static String requiredText(Map<Object, Object> source, String key) {
        Object value = source.get(key);
        if (value == null) {
            throw new IllegalArgumentException("missing required hash field: " + key);
        }
        String text = String.valueOf(value);
        if (text.isBlank()) {
            throw new IllegalArgumentException("blank required hash field: " + key);
        }
        return text;
    }

    private static Long requiredLong(Map<Object, Object> source, String key) {
        return Long.parseLong(requiredText(source, key));
    }

    private static Instant requiredInstant(Map<Object, Object> source, String key) {
        return Instant.ofEpochMilli(requiredLong(source, key));
    }

    private static Instant optionalInstant(Map<Object, Object> source, String key) {
        Object value = source.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        if (text.isBlank()) {
            return null;
        }
        return Instant.ofEpochMilli(Long.parseLong(text));
    }
}
