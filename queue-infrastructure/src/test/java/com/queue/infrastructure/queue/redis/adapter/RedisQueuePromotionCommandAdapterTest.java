package com.queue.infrastructure.queue.redis.adapter;

import com.queue.application.dto.PromoteCommand;
import com.queue.application.dto.PromoteResult;
import com.queue.infrastructure.queue.redis.generator.RedisQueueKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RedisQueuePromotionCommandAdapterTest {

    private StringRedisTemplate stringRedisTemplate;
    private RedisScript<Long> promoteWaitingEntriesScript;
    private RedisQueueKeyGenerator keyGenerator;
    private RedisQueuePromotionCommandAdapter adapter;

    @BeforeEach
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        promoteWaitingEntriesScript = mock(RedisScript.class);
        keyGenerator = mock(RedisQueueKeyGenerator.class);

        when(keyGenerator.waitingQueueKey("queue-1")).thenReturn("queue:waiting:queue-1");
        when(keyGenerator.activeQueueKey("queue-1")).thenReturn("queue:active:queue-1");
        when(keyGenerator.activeExpiryKey("queue-1")).thenReturn("queue:active-expiry:queue-1");
        when(keyGenerator.entryKeyPrefix()).thenReturn("queue:entry:");

        adapter = new RedisQueuePromotionCommandAdapter(
                stringRedisTemplate,
                promoteWaitingEntriesScript,
                keyGenerator
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
        )).thenReturn(2L);

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
}