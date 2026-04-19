package com.queue.infrastructure.queue.redis.adapter;

import com.queue.application.port.out.QueueQueryPort;
import com.queue.infrastructure.queue.redis.generator.RedisQueueKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RedisQueueQueryAdapterTest.TestConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisQueueQueryAdapterTest {

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
            RedisQueueQueryAdapter.class,
            RedisQueueKeyGenerator.class
    })
    static class TestConfig {
    }

    @Autowired
    private QueueQueryPort queueQueryPort;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisQueueKeyGenerator keyGenerator;

    @BeforeEach
    void clear() {
        stringRedisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushAll();
    }

    @Test
    void findRank_whenMemberExists_thenReturnOneBasedRank() {
        String waitingQueueKey = keyGenerator.waitingQueueKey("queue-1");

        stringRedisTemplate.opsForZSet().add(waitingQueueKey, "token-1", 1);
        stringRedisTemplate.opsForZSet().add(waitingQueueKey, "token-2", 2);
        stringRedisTemplate.opsForZSet().add(waitingQueueKey, "token-3", 3);

        Long rank = queueQueryPort.findRank("queue-1", "token-2");

        assertThat(rank).isEqualTo(2L);
    }

    @Test
    void findRank_whenMemberDoesNotExist_thenReturnNull() {
        Long rank = queueQueryPort.findRank("queue-1", "missing-token");

        assertThat(rank).isNull();
    }
}