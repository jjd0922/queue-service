package com.queue.infrastructure.queue.redis.generator;

import org.springframework.stereotype.Component;

@Component
public class RedisQueueKeyGenerator {

    private static final String WAITING_QUEUE_KEY = "queue:waiting:%s";
    private static final String ENTRY_KEY = "queue:entry:%s";
    private static final String USER_INDEX_KEY = "queue:user-index:%s:%s";
    private static final String SEQUENCE_KEY = "queue:sequence:%s";
    private static final String ENTRY_KEY_PREFIX = "queue:entry:";

    public String waitingQueueKey(String queueId) {
        return WAITING_QUEUE_KEY.formatted(queueId);
    }

    public String entryKey(String token) {
        return ENTRY_KEY.formatted(token);
    }

    public String userIndexKey(String queueId, Long userId) {
        return USER_INDEX_KEY.formatted(queueId, userId);
    }

    public String sequenceKey(String queueId) {
        return SEQUENCE_KEY.formatted(queueId);
    }

    public String entryKeyPrefix() {
        return ENTRY_KEY_PREFIX;
    }
}
