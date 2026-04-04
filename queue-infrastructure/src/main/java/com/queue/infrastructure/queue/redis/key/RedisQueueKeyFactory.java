package com.queue.infrastructure.queue.redis.key;

public final class RedisQueueKeyFactory {

    private static final String PREFIX = "queue:v1";

    private RedisQueueKeyFactory() {
    }

    private static String slot(String queueId) {
        return "{" + queueId + "}";
    }

    public static String sequenceKey(String queueId) {
        return PREFIX + ":" + slot(queueId) + ":seq";
    }

    public static String waitingKey(String queueId) {
        return PREFIX + ":" + slot(queueId) + ":waiting";
    }

    public static String activeKey(String queueId) {
        return PREFIX + ":" + slot(queueId) + ":active";
    }

    public static String entryKey(String queueId, String token) {
        return PREFIX + ":" + slot(queueId) + ":entry:" + token;
    }

    public static String userKey(String queueId, Long userId) {
        return PREFIX + ":" + slot(queueId) + ":user:" + userId;
    }
}