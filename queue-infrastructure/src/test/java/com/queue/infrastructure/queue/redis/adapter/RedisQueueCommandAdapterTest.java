package com.queue.infrastructure.queue.redis.adapter;

import com.queue.application.dto.EnqueueCommand;
import com.queue.application.dto.PromoteCommand;
import com.queue.application.dto.PromoteResult;
import com.queue.application.port.out.QueueCommandPort;
import com.queue.domain.model.EnqueueDecision;
import com.queue.infrastructure.config.RedisQueueScriptConfig;
import com.queue.infrastructure.queue.redis.generator.RedisQueueKeyGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RedisQueueCommandAdapterTest.TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisQueueCommandAdapterTest {

    private static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7.2-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        if (!redis.isRunning()) {
            redis.start();
        }

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            RedisQueueCommandAdapter.class,
            RedisQueueScriptConfig.class,
            RedisQueueKeyGenerator.class
    })
    static class TestConfig {
    }

    @Autowired
    private QueueCommandPort queueCommandPort;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisQueueKeyGenerator keyGenerator;

    @BeforeEach
    void clear() {
        Assertions.assertNotNull(stringRedisTemplate.getConnectionFactory());
        stringRedisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushAll();
    }

    @DisplayName("동일 사용자가 재시도하면 하나의 엔트리만 생성된다")
    @Test
    void enqueueOrGetExisting_whenSameUserRetries_thenSingleEntryOnly() {
        EnqueueCommand first = new EnqueueCommand(
                "queue-1",
                1L,
                Instant.parse("2026-04-05T00:00:00Z")
        );

        EnqueueCommand second = new EnqueueCommand(
                "queue-1",
                1L,
                Instant.parse("2026-04-05T00:00:01Z")
        );

        EnqueueDecision firstResult = queueCommandPort.enqueueOrGetExisting(first);
        EnqueueDecision secondResult = queueCommandPort.enqueueOrGetExisting(second);

        assertThat(firstResult.outcome().name()).isEqualTo("CREATED");
        assertThat(secondResult.outcome().name()).isEqualTo("ALREADY_WAITING");
        assertThat(firstResult.entry().getToken()).isEqualTo(secondResult.entry().getToken());

        Long zsetSize = stringRedisTemplate.opsForZSet()
                .zCard(keyGenerator.waitingQueueKey("queue-1"));

        assertThat(zsetSize).isEqualTo(1L);
    }

    @DisplayName("user index 가 stale 상태면 새 엔트리를 다시 생성한다")
    @Test
    void enqueueOrGetExisting_whenUserKeyIsStale_thenRecreateEntry() {
        String staleToken = "stale-token";

        stringRedisTemplate.opsForValue().set(
                keyGenerator.userIndexKey("queue-1", 1L),
                staleToken
        );

        EnqueueDecision result = queueCommandPort.enqueueOrGetExisting(
                new EnqueueCommand(
                        "queue-1",
                        1L,
                        Instant.parse("2026-04-05T00:00:00Z")
                )
        );

        String indexedToken = stringRedisTemplate.opsForValue().get(
                keyGenerator.userIndexKey("queue-1", 1L)
        );

        assertThat(result.outcome().name()).isEqualTo("CREATED");
        assertThat(result.entry().getToken()).isNotEqualTo(staleToken);
        assertThat(indexedToken).isEqualTo(result.entry().getToken());
    }

    @DisplayName("여러 사용자가 순차 진입하면 sequence 가 증가한다")
    @Test
    void enqueueOrGetExisting_whenMultipleUsersSequentially_thenSequenceIncreases() {
        EnqueueDecision first = queueCommandPort.enqueueOrGetExisting(
                new EnqueueCommand("queue-1", 1L, Instant.parse("2026-04-05T00:00:00Z"))
        );

        EnqueueDecision second = queueCommandPort.enqueueOrGetExisting(
                new EnqueueCommand("queue-1", 2L, Instant.parse("2026-04-05T00:00:01Z"))
        );

        EnqueueDecision third = queueCommandPort.enqueueOrGetExisting(
                new EnqueueCommand("queue-1", 3L, Instant.parse("2026-04-05T00:00:02Z"))
        );

        assertThat(first.entry().getSequence()).isEqualTo(1L);
        assertThat(second.entry().getSequence()).isEqualTo(2L);
        assertThat(third.entry().getSequence()).isEqualTo(3L);
    }

    @DisplayName("동일 사용자가 동시에 요청해도 하나의 엔트리만 생성된다")
    @Test
    void enqueueOrGetExisting_whenSameUserConcurrentRequests_thenOnlyOneCreated() throws Exception {
        int threadCount = 20;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<EnqueueDecision>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(() -> {
                ready.countDown();
                start.await();

                return queueCommandPort.enqueueOrGetExisting(
                        new EnqueueCommand("queue-1", 1L, Instant.now())
                );
            }));
        }

        ready.await();
        start.countDown();

        List<EnqueueDecision> results = new ArrayList<>();
        for (Future<EnqueueDecision> future : futures) {
            results.add(future.get());
        }

        executorService.shutdown();

        long createdCount = results.stream()
                .filter(result -> "CREATED".equals(result.outcome().name()))
                .count();

        long distinctTokenCount = results.stream()
                .map(result -> result.entry().getToken())
                .distinct()
                .count();

        Long zsetSize = stringRedisTemplate.opsForZSet()
                .zCard(keyGenerator.waitingQueueKey("queue-1"));

        assertThat(createdCount).isEqualTo(1L);
        assertThat(distinctTokenCount).isEqualTo(1L);
        assertThat(zsetSize).isEqualTo(1L);
    }

    @DisplayName("서로 다른 사용자가 동시에 요청하면 모두 생성된다")
    @Test
    void enqueueOrGetExisting_whenDifferentUsersConcurrentRequests_thenAllCreated() throws Exception {
        int threadCount = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<EnqueueDecision>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1L;
            futures.add(executorService.submit(() -> {
                ready.countDown();
                start.await();

                return queueCommandPort.enqueueOrGetExisting(
                        new EnqueueCommand("queue-1", userId, Instant.now())
                );
            }));
        }

        ready.await();
        start.countDown();

        List<EnqueueDecision> results = new ArrayList<>();
        for (Future<EnqueueDecision> future : futures) {
            results.add(future.get());
        }

        executorService.shutdown();

        long createdCount = results.stream()
                .filter(result -> "CREATED".equals(result.outcome().name()))
                .count();

        long distinctTokenCount = results.stream()
                .map(result -> result.entry().getToken())
                .distinct()
                .count();

        Long zsetSize = stringRedisTemplate.opsForZSet()
                .zCard(keyGenerator.waitingQueueKey("queue-1"));

        assertThat(createdCount).isEqualTo(threadCount);
        assertThat(distinctTokenCount).isEqualTo(threadCount);
        assertThat(zsetSize).isEqualTo(threadCount);
    }

    @DisplayName("대기열 승격 시 수용 가능 인원만큼만 active 로 이동한다")
    @Test
    void promoteWaitingEntries_whenSlotsAvailable_thenPromoteWithinCapacity() {
        queueCommandPort.enqueueOrGetExisting(
                new EnqueueCommand("queue-1", 1L, Instant.parse("2026-04-05T00:00:00Z"))
        );
        queueCommandPort.enqueueOrGetExisting(
                new EnqueueCommand("queue-1", 2L, Instant.parse("2026-04-05T00:00:01Z"))
        );
        queueCommandPort.enqueueOrGetExisting(
                new EnqueueCommand("queue-1", 3L, Instant.parse("2026-04-05T00:00:02Z"))
        );

        PromoteResult result = queueCommandPort.promoteWaitingEntries(
                new PromoteCommand(
                        "queue-1",
                        Instant.parse("2026-04-05T00:01:00Z"),
                        2,
                        10,
                        Duration.ofSeconds(180)
                )
        );

        Long waitingSize = stringRedisTemplate.opsForZSet()
                .zCard(keyGenerator.waitingQueueKey("queue-1"));
        Long activeSize = stringRedisTemplate.opsForZSet()
                .zCard(keyGenerator.activeQueueKey("queue-1"));

        assertThat(result.queueId()).isEqualTo("queue-1");
        assertThat(result.requestedCount()).isEqualTo(10);
        assertThat(result.promotedCount()).isEqualTo(2);
        assertThat(waitingSize).isEqualTo(1L);
        assertThat(activeSize).isEqualTo(2L);
    }

    @DisplayName("active 가 이미 가득 차 있으면 승격되지 않는다")
    @Test
    void promoteWaitingEntries_whenActiveIsFull_thenPromoteNothing() {
        queueCommandPort.enqueueOrGetExisting(
                new EnqueueCommand("queue-1", 1L, Instant.parse("2026-04-05T00:00:00Z"))
        );
        queueCommandPort.enqueueOrGetExisting(
                new EnqueueCommand("queue-1", 2L, Instant.parse("2026-04-05T00:00:01Z"))
        );

        PromoteResult firstPromotion = queueCommandPort.promoteWaitingEntries(
                new PromoteCommand(
                        "queue-1",
                        Instant.parse("2026-04-05T00:01:00Z"),
                        2,
                        10,
                        Duration.ofSeconds(180)
                )
        );

        PromoteResult secondPromotion = queueCommandPort.promoteWaitingEntries(
                new PromoteCommand(
                        "queue-1",
                        Instant.parse("2026-04-05T00:02:00Z"),
                        2,
                        10,
                        Duration.ofSeconds(180)
                )
        );

        Long waitingSize = stringRedisTemplate.opsForZSet()
                .zCard(keyGenerator.waitingQueueKey("queue-1"));
        Long activeSize = stringRedisTemplate.opsForZSet()
                .zCard(keyGenerator.activeQueueKey("queue-1"));

        assertThat(firstPromotion.promotedCount()).isEqualTo(2);
        assertThat(secondPromotion.promotedCount()).isZero();
        assertThat(waitingSize).isZero();
        assertThat(activeSize).isEqualTo(2L);
    }

    @DisplayName("승격 배치 크기보다 많은 대기 인원이 있어도 배치 크기만큼만 승격된다")
    @Test
    void promoteWaitingEntries_whenWaitingExceedsBatchSize_thenPromoteOnlyBatchSize() {
        queueCommandPort.enqueueOrGetExisting(
                new EnqueueCommand("queue-1", 1L, Instant.parse("2026-04-05T00:00:00Z"))
        );
        queueCommandPort.enqueueOrGetExisting(
                new EnqueueCommand("queue-1", 2L, Instant.parse("2026-04-05T00:00:01Z"))
        );
        queueCommandPort.enqueueOrGetExisting(
                new EnqueueCommand("queue-1", 3L, Instant.parse("2026-04-05T00:00:02Z"))
        );
        queueCommandPort.enqueueOrGetExisting(
                new EnqueueCommand("queue-1", 4L, Instant.parse("2026-04-05T00:00:03Z"))
        );
        queueCommandPort.enqueueOrGetExisting(
                new EnqueueCommand("queue-1", 5L, Instant.parse("2026-04-05T00:00:04Z"))
        );

        PromoteResult result = queueCommandPort.promoteWaitingEntries(
                new PromoteCommand(
                        "queue-1",
                        Instant.parse("2026-04-05T00:01:00Z"),
                        10,
                        2,
                        Duration.ofSeconds(180)
                )
        );

        Long waitingSize = stringRedisTemplate.opsForZSet()
                .zCard(keyGenerator.waitingQueueKey("queue-1"));
        Long activeSize = stringRedisTemplate.opsForZSet()
                .zCard(keyGenerator.activeQueueKey("queue-1"));

        assertThat(result.requestedCount()).isEqualTo(2);
        assertThat(result.promotedCount()).isEqualTo(2);
        assertThat(waitingSize).isEqualTo(3L);
        assertThat(activeSize).isEqualTo(2L);
    }
}