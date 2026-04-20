package com.queue.infrastructure.queue.redis.adapter;

import com.queue.application.dto.command.PromoteCommand;
import com.queue.application.dto.result.PromoteResult;
import com.queue.domain.model.QueueEntryStatus;
import com.queue.infrastructure.queue.redis.generator.RedisQueueKeyGenerator;
import com.queue.infrastructure.queue.redis.mapper.RedisQueueEntryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RedisQueuePromotionCommandAdapterTest {

    private StringRedisTemplate stringRedisTemplate;
    private RedisScript<List> promoteWaitingEntriesScript;
    private RedisQueueKeyGenerator keyGenerator;
    private RedisQueueEntryMapper redisQueueEntryMapper;
    private HashOperations<String, Object, Object> hashOperations;
    private RedisQueuePromotionCommandAdapter adapter;

    @BeforeEach
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        promoteWaitingEntriesScript = mock(RedisScript.class);
        keyGenerator = mock(RedisQueueKeyGenerator.class);
        redisQueueEntryMapper = new RedisQueueEntryMapper();
        hashOperations = mock(HashOperations.class);

        when(keyGenerator.waitingQueueKey("queue-1")).thenReturn("queue:waiting:queue-1");
        when(keyGenerator.activeQueueKey("queue-1")).thenReturn("queue:active:queue-1");
        when(keyGenerator.activeExpiryKey("queue-1")).thenReturn("queue:active-expiry:queue-1");
        when(keyGenerator.entryKeyPrefix()).thenReturn("queue:entry:");
        when(keyGenerator.entryKey("token-1")).thenReturn("queue:entry:token-1");
        when(keyGenerator.entryKey("token-2")).thenReturn("queue:entry:token-2");
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

        adapter = new RedisQueuePromotionCommandAdapter(
                stringRedisTemplate,
                promoteWaitingEntriesScript,
                keyGenerator,
                redisQueueEntryMapper
        );
    }

    @Test
    @DisplayName("승격 요청 시 redis key 와 arg 를 구성하고 PromoteResult 를 반환한다")
    void promoteWaitingEntries_buildsKeysAndReturnsResult() {
        Instant now = Instant.parse("2026-04-05T00:01:00Z");

        PromoteCommand command = new PromoteCommand(
                "queue-1",
                now,
                10,
                2,
                Duration.ofSeconds(180)
        );

        when(stringRedisTemplate.execute(
                eq(promoteWaitingEntriesScript),
                eq(List.of(
                        "queue:waiting:queue-1",
                        "queue:active:queue-1",
                        "queue:active-expiry:queue-1"
                )),
                eq("queue:entry:"),
                eq("ACTIVE"),
                eq("2026-04-05T00:01:00Z"),
                eq("2026-04-05T00:04:00Z"),
                eq(String.valueOf(Instant.parse("2026-04-05T00:04:00Z").toEpochMilli())),
                eq("10"),
                eq("2")
        )).thenReturn(List.of("token-1", "token-2"));

        when(hashOperations.entries("queue:entry:token-1")).thenReturn(entryHash("token-1", 1L, 1L, now));
        when(hashOperations.entries("queue:entry:token-2")).thenReturn(entryHash("token-2", 2L, 2L, now));

        PromoteResult result = adapter.promoteWaitingEntries(command);

        assertThat(result.queueId()).isEqualTo("queue-1");
        assertThat(result.requestedCount()).isEqualTo(2);
        assertThat(result.promotedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("redis 결과가 null 이면 promotedCount 를 0 으로 반환한다")
    void promoteWaitingEntries_returnsZero_whenRedisReturnsNull() {
        Instant now = Instant.parse("2026-04-05T00:01:00Z");

        PromoteCommand command = new PromoteCommand(
                "queue-1",
                now,
                10,
                2,
                Duration.ofSeconds(180)
        );

        when(stringRedisTemplate.execute(
                eq(promoteWaitingEntriesScript),
                anyList(),
                any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(null);

        PromoteResult result = adapter.promoteWaitingEntries(command);

        assertThat(result.queueId()).isEqualTo("queue-1");
        assertThat(result.requestedCount()).isEqualTo(2);
        assertThat(result.promotedCount()).isZero();
    }

    private Map<Object, Object> entryHash(String token, Long userId, Long sequence, Instant now) {
        return Map.of(
                "token", token,
                "userId", userId.toString(),
                "status", QueueEntryStatus.ACTIVE.name(),
                "sequence", sequence.toString(),
                "enteredAt", now.minusSeconds(10).toString(),
                "activatedAt", now.toString(),
                "expiresAt", now.plusSeconds(180).toString(),
                "lastUpdatedAt", now.toString()
        );
    }
}
