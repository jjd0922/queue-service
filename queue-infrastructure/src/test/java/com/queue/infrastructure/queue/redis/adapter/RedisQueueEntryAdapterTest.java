package com.queue.infrastructure.queue.redis.adapter;

import com.queue.domain.model.QueueEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisQueueEntryAdapterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisQueueEntryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisQueueEntryAdapter(redisTemplate);
    }

    @Test
    @DisplayName("token 으로 entry hash 를 조회하면 QueueEntry 를 반환한다")
    void findByToken() {
        Map<Object, Object> hash = new HashMap<>();
        hash.put("token", "qt_token_1");
        hash.put("queueId", "product:100");
        hash.put("userId", "1");
        hash.put("status", "WAITING");
        hash.put("sequence", "10");
        hash.put("enteredAt", String.valueOf(Instant.parse("2026-04-04T10:00:00Z").toEpochMilli()));
        hash.put("lastUpdatedAt", String.valueOf(Instant.parse("2026-04-04T10:00:00Z").toEpochMilli()));

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("queue:v1:{product:100}:entry:qt_token_1"))
                .thenReturn(hash);

        Optional<QueueEntry> result = adapter.findByToken("product:100", "qt_token_1");

        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("qt_token_1");
        assertThat(result.get().getQueueId()).isEqualTo("product:100");
        assertThat(result.get().getUserId()).isEqualTo(1L);
        assertThat(result.get().getSequence()).isEqualTo(10L);
        assertThat(result.get().isWaiting()).isTrue();
    }

    @Test
    @DisplayName("token 으로 조회한 entry hash 가 비어 있으면 empty 를 반환한다")
    void findByToken_whenHashEmpty() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("queue:v1:{product:100}:entry:qt_token_1"))
                .thenReturn(Map.of());

        Optional<QueueEntry> result = adapter.findByToken("product:100", "qt_token_1");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("queueId 와 userId 로 token 을 찾은 뒤 entry 를 조회한다")
    void findByQueueIdAndUserId() {
        Map<Object, Object> hash = new HashMap<>();
        hash.put("token", "qt_token_1");
        hash.put("queueId", "product:100");
        hash.put("userId", "1");
        hash.put("status", "WAITING");
        hash.put("sequence", "10");
        hash.put("enteredAt", String.valueOf(Instant.parse("2026-04-04T10:00:00Z").toEpochMilli()));
        hash.put("lastUpdatedAt", String.valueOf(Instant.parse("2026-04-04T10:00:00Z").toEpochMilli()));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("queue:v1:{product:100}:user:1"))
                .thenReturn("qt_token_1");

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("queue:v1:{product:100}:entry:qt_token_1"))
                .thenReturn(hash);

        Optional<QueueEntry> result = adapter.findByQueueIdAndUserId("product:100", 1L);

        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("qt_token_1");
        assertThat(result.get().getQueueId()).isEqualTo("product:100");
        assertThat(result.get().getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("userKey 에 token 이 없으면 empty 를 반환한다")
    void findByQueueIdAndUserId_whenUserKeyMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("queue:v1:{product:100}:user:1"))
                .thenReturn(null);

        Optional<QueueEntry> result = adapter.findByQueueIdAndUserId("product:100", 1L);

        assertThat(result).isEmpty();
        verify(redisTemplate, never()).opsForHash();
    }

    @Test
    @DisplayName("entry 를 저장하면 entry hash 와 userKey token 을 함께 저장한다")
    void save() {
        Instant now = Instant.parse("2026-04-04T10:00:00Z");
        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, now);

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        adapter.save(entry);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> hashCaptor = ArgumentCaptor.forClass(Map.class);

        verify(hashOperations).putAll(
                eq("queue:v1:{product:100}:entry:qt_token_1"),
                hashCaptor.capture()
        );
        verify(valueOperations).set("queue:v1:{product:100}:user:1", "qt_token_1");

        Map<String, String> savedHash = hashCaptor.getValue();
        assertThat(savedHash)
                .containsEntry("token", "qt_token_1")
                .containsEntry("queueId", "product:100")
                .containsEntry("userId", "1")
                .containsEntry("status", "WAITING")
                .containsEntry("sequence", "10")
                .containsEntry("enteredAt", String.valueOf(now.toEpochMilli()))
                .containsEntry("lastUpdatedAt", String.valueOf(now.toEpochMilli()));
    }
}