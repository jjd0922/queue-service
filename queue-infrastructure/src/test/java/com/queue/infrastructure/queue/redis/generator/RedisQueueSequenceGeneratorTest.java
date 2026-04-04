package com.queue.infrastructure.queue.redis.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisQueueSequenceGeneratorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisQueueSequenceGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new RedisQueueSequenceGenerator(redisTemplate);
    }

    @Test
    @DisplayName("queue 별 sequence 를 증가시켜 반환한다")
    void nextSequence() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("queue:v1:{product:100}:seq"))
                .thenReturn(11L);

        Long sequence = generator.nextSequence("product:100");

        assertThat(sequence).isEqualTo(11L);
    }

    @Test
    @DisplayName("sequence 증가 결과가 null 이면 예외가 발생한다")
    void nextSequence_fail_whenNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("queue:v1:{product:100}:seq"))
                .thenReturn(null);

        assertThatThrownBy(() -> generator.nextSequence("product:100"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("failed to generate queue sequence");
    }

    @Test
    @DisplayName("sequence 증가 결과가 0 이하이면 예외가 발생한다")
    void nextSequence_fail_whenNonPositive() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("queue:v1:{product:100}:seq"))
                .thenReturn(0L);

        assertThatThrownBy(() -> generator.nextSequence("product:100"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("failed to generate queue sequence");
    }
}