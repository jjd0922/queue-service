package com.queue.infrastructure.queue.redis.adapter;

import com.queue.application.dto.command.ExpireCommand;
import com.queue.application.dto.result.ExpireResult;
import com.queue.infrastructure.queue.redis.generator.RedisQueueKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RedisQueueExpirationCommandAdapterTest {

    private StringRedisTemplate stringRedisTemplate;
    private RedisScript<Long> expireActiveEntriesScript;
    private RedisQueueKeyGenerator keyGenerator;
    private RedisQueueExpirationCommandAdapter adapter;

    @BeforeEach
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        expireActiveEntriesScript = mock(RedisScript.class);
        keyGenerator = mock(RedisQueueKeyGenerator.class);

        when(keyGenerator.activeExpiryKey("queue-1")).thenReturn("queue:active-expiry:queue-1");
        when(keyGenerator.activeQueueKey("queue-1")).thenReturn("queue:active:queue-1");
        when(keyGenerator.entryKeyPrefix()).thenReturn("queue:entry:");
        when(keyGenerator.userIndexKeyPrefix("queue-1")).thenReturn("queue:user-index:queue-1:");

        adapter = new RedisQueueExpirationCommandAdapter(
                stringRedisTemplate,
                expireActiveEntriesScript,
                keyGenerator
        );
    }

    @Test
    @DisplayName("만료 요청 시 redis key 와 arg 를 구성하고 ExpireResult 를 반환한다")
    void expireActiveEntries_buildsKeysAndReturnsResult() {
        Instant now = Instant.parse("2026-04-16T10:00:00Z");

        ExpireCommand command = new ExpireCommand(
                "queue-1",
                now,
                50
        );

        when(stringRedisTemplate.execute(
                eq(expireActiveEntriesScript),
                eq(List.of(
                        "queue:active-expiry:queue-1",
                        "queue:active:queue-1"
                )),
                eq("queue:entry:"),
                eq("EXPIRED"),
                eq("2026-04-16T10:00:00Z"),
                eq(String.valueOf(now.toEpochMilli())),
                eq("50"),
                eq("queue:user-index:queue-1:")
        )).thenReturn(3L);

        ExpireResult result = adapter.expireActiveEntries(command);

        assertThat(result.queueId()).isEqualTo("queue-1");
        assertThat(result.requestedBatchSize()).isEqualTo(50);
        assertThat(result.actualExpiredCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("redis 결과가 null 이면 actualExpiredCount 를 0 으로 반환한다")
    void expireActiveEntries_returnsZero_whenRedisReturnsNull() {
        Instant now = Instant.parse("2026-04-16T10:00:00Z");

        ExpireCommand command = new ExpireCommand(
                "queue-1",
                now,
                50
        );

        when(stringRedisTemplate.execute(
                eq(expireActiveEntriesScript),
                anyList(),
                any(), any(), any(), any(), any(), any()
        )).thenReturn(null);

        ExpireResult result = adapter.expireActiveEntries(command);

        assertThat(result.queueId()).isEqualTo("queue-1");
        assertThat(result.requestedBatchSize()).isEqualTo(50);
        assertThat(result.actualExpiredCount()).isZero();
    }
}