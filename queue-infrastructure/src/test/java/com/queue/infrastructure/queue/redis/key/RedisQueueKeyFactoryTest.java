package com.queue.infrastructure.queue.redis.key;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisQueueKeyFactoryTest {

    @Test
    @DisplayName("sequenceKey 를 생성한다")
    void sequenceKey() {
        String key = RedisQueueKeyFactory.sequenceKey("product:100");

        assertThat(key).isEqualTo("queue:v1:{product:100}:seq");
    }

    @Test
    @DisplayName("waitingKey 를 생성한다")
    void waitingKey() {
        String key = RedisQueueKeyFactory.waitingKey("product:100");

        assertThat(key).isEqualTo("queue:v1:{product:100}:waiting");
    }

    @Test
    @DisplayName("activeKey 를 생성한다")
    void activeKey() {
        String key = RedisQueueKeyFactory.activeKey("product:100");

        assertThat(key).isEqualTo("queue:v1:{product:100}:active");
    }

    @Test
    @DisplayName("entryKey 를 생성한다")
    void entryKey() {
        String key = RedisQueueKeyFactory.entryKey("product:100", "qt_token_1");

        assertThat(key).isEqualTo("queue:v1:{product:100}:entry:qt_token_1");
    }

    @Test
    @DisplayName("userKey 를 생성한다")
    void userKey() {
        String key = RedisQueueKeyFactory.userKey("product:100", 99L);

        assertThat(key).isEqualTo("queue:v1:{product:100}:user:99");
    }
}