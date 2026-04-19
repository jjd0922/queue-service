package com.queue.infrastructure.queue.redis.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisWaitingQueueAdapterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private RedisWaitingQueueAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisWaitingQueueAdapter(redisTemplate);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    @DisplayName("waiting queue 에 token 을 enqueue 한다")
    void enqueue() {
        adapter.enqueue("product:100", "qt_token_1", 10L);

        verify(zSetOperations).add("queue:v1:{product:100}:waiting", "qt_token_1", 10D);
    }

    @Test
    @DisplayName("waiting queue rank 를 position 으로 반환한다")
    void findPosition() {
        when(zSetOperations.rank("queue:v1:{product:100}:waiting", "qt_token_1"))
                .thenReturn(0L);

        Optional<Long> position = adapter.findPosition("product:100", "qt_token_1");

        assertThat(position).contains(1L);
    }

    @Test
    @DisplayName("waiting queue 에 token 이 없으면 empty 를 반환한다")
    void findPosition_whenMissing() {
        when(zSetOperations.rank("queue:v1:{product:100}:waiting", "qt_token_1"))
                .thenReturn(null);

        Optional<Long> position = adapter.findPosition("product:100", "qt_token_1");

        assertThat(position).isEmpty();
    }

    @Test
    @DisplayName("waiting queue 에서 token 을 제거한다")
    void remove() {
        adapter.remove("product:100", "qt_token_1");

        verify(zSetOperations).remove("queue:v1:{product:100}:waiting", "qt_token_1");
    }

    @Test
    @DisplayName("waiting queue size 를 반환한다")
    void count() {
        when(zSetOperations.size("queue:v1:{product:100}:waiting"))
                .thenReturn(3L);

        long count = adapter.count("product:100");

        assertThat(count).isEqualTo(3L);
    }
}