package com.queue.infrastructure.queue.redis.adapter;

import com.queue.domain.model.QueueEntrySnapshot;
import com.queue.domain.model.QueueEntryStatus;
import com.queue.infrastructure.queue.redis.generator.RedisQueueKeyGenerator;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.utility.DockerImageName.parse;

@Testcontainers
class RedisQueueStatusQueryAdapterTest {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(parse("redis:7.2-alpine"))
                    .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private RedisQueueKeyGenerator keyGenerator;
    private RedisQueueStatusQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(
                REDIS.getHost(),
                REDIS.getMappedPort(6379)
        );
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.afterPropertiesSet();

        keyGenerator = new RedisQueueKeyGenerator();
        adapter = new RedisQueueStatusQueryAdapter(redisTemplate, keyGenerator);

        Assertions.assertNotNull(redisTemplate.getConnectionFactory());
        redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushAll();
    }

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Nested
    @DisplayName("findEntry")
    class FindEntry {

        @Test
        @DisplayName("엔트리 해시가 존재하면 스냅샷을 반환한다")
        void returnsSnapshotWhenEntryExists() {
            // given
            String token = "token-1";

            redisTemplate.opsForHash().putAll(
                    keyGenerator.entryKey(token),
                    Map.of(
                            "status", "WAITING",
                            "enteredAt", "2026-04-06T10:00:00Z"
                    )
            );

            // when
            Optional<QueueEntrySnapshot> result = adapter.findEntry(token);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().queueToken()).isEqualTo(token);
            assertThat(result.get().status()).isEqualTo(QueueEntryStatus.WAITING);
            assertThat(result.get().enteredAt()).hasToString("2026-04-06T10:00:00Z");
            assertThat(result.get().activatedAt()).isNull();
            assertThat(result.get().expiresAt()).isNull();
        }

        @Test
        @DisplayName("엔트리 해시가 없으면 빈 값을 반환한다")
        void returnsEmptyWhenEntryDoesNotExist() {
            // given
            String token = "missing-token";

            // when
            Optional<QueueEntrySnapshot> result = adapter.findEntry(token);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findWaitingRank")
    class FindWaitingRank {

        @Test
        @DisplayName("대기열에 토큰이 있으면 순번을 반환한다")
        void returnsRankWhenTokenExists() {
            // given
            String queueName = "concert-queue";
            String token = "token-2";

            String waitingKey = keyGenerator.waitingQueueKey(queueName);
            redisTemplate.opsForZSet().add(waitingKey, "token-1", 1);
            redisTemplate.opsForZSet().add(waitingKey, token, 2);
            redisTemplate.opsForZSet().add(waitingKey, "token-3", 3);

            // when
            Optional<Long> result = adapter.findWaitingRank(queueName, token);

            // then
            assertThat(result).contains(1L);
        }

        @Test
        @DisplayName("대기열에 토큰이 없으면 빈 값을 반환한다")
        void returnsEmptyWhenTokenDoesNotExist() {
            // given
            String queueName = "concert-queue";
            String token = "missing-token";

            // when
            Optional<Long> result = adapter.findWaitingRank(queueName, token);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("isActive")
    class IsActive {

        @Test
        @DisplayName("활성 대기열에 토큰이 있으면 true를 반환한다")
        void returnsTrueWhenTokenExistsInActiveQueue() {
            // given
            String queueName = "concert-queue";
            String token = "token-1";

            redisTemplate.opsForZSet().add(keyGenerator.activeQueueKey(queueName), token, 1);

            // when
            boolean result = adapter.isActive(queueName, token);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("활성 대기열에 토큰이 없으면 false를 반환한다")
        void returnsFalseWhenTokenDoesNotExistInActiveQueue() {
            // given
            String queueName = "concert-queue";
            String token = "missing-token";

            // when
            boolean result = adapter.isActive(queueName, token);

            // then
            assertThat(result).isFalse();
        }
    }
}