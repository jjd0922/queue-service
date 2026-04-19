package com.queue.infrastructure.redis.queue;

import com.queue.domain.queue.model.QueueEntry;
import com.queue.domain.queue.model.QueueStatus;
import com.queue.infrastructure.queue.redis.RedisQueueEntryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisQueueEntryMapperTest {

    private final RedisQueueEntryMapper mapper = new RedisQueueEntryMapper();

    @Test
    @DisplayName("WAITING 상태 엔트리를 hash 로 변환한다")
    void toHash_waiting() {
        Instant now = Instant.parse("2026-04-04T10:00:00Z");
        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, now);

        Map<String, String> hash = mapper.toHash(entry);

        assertThat(hash)
                .containsEntry("token", "qt_token_1")
                .containsEntry("queueId", "product:100")
                .containsEntry("userId", "1")
                .containsEntry("status", "WAITING")
                .containsEntry("sequence", "10")
                .containsEntry("enteredAt", String.valueOf(now.toEpochMilli()))
                .containsEntry("lastUpdatedAt", String.valueOf(now.toEpochMilli()));

        assertThat(hash).doesNotContainKeys("activatedAt", "expiresAt");
    }

    @Test
    @DisplayName("ACTIVE 상태 엔트리를 hash 로 변환한다")
    void toHash_active() {
        Instant enteredAt = Instant.parse("2026-04-04T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-04-04T10:01:00Z");
        Instant expiresAt = Instant.parse("2026-04-04T10:06:00Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, enteredAt);
        entry.activate(activatedAt, expiresAt);

        Map<String, String> hash = mapper.toHash(entry);

        assertThat(hash)
                .containsEntry("token", "qt_token_1")
                .containsEntry("queueId", "product:100")
                .containsEntry("userId", "1")
                .containsEntry("status", "ACTIVE")
                .containsEntry("sequence", "10")
                .containsEntry("enteredAt", String.valueOf(enteredAt.toEpochMilli()))
                .containsEntry("activatedAt", String.valueOf(activatedAt.toEpochMilli()))
                .containsEntry("expiresAt", String.valueOf(expiresAt.toEpochMilli()))
                .containsEntry("lastUpdatedAt", String.valueOf(activatedAt.toEpochMilli()));
    }

    @Test
    @DisplayName("WAITING 상태 hash 를 QueueEntry 로 복원한다")
    void fromHash_waiting() {
        Instant now = Instant.parse("2026-04-04T10:00:00Z");

        Map<Object, Object> hash = new HashMap<>();
        hash.put("token", "qt_token_1");
        hash.put("queueId", "product:100");
        hash.put("userId", "1");
        hash.put("status", "WAITING");
        hash.put("sequence", "10");
        hash.put("enteredAt", String.valueOf(now.toEpochMilli()));
        hash.put("lastUpdatedAt", String.valueOf(now.toEpochMilli()));

        QueueEntry entry = mapper.fromHash(hash);

        assertThat(entry.getToken()).isEqualTo("qt_token_1");
        assertThat(entry.getQueueId()).isEqualTo("product:100");
        assertThat(entry.getUserId()).isEqualTo(1L);
        assertThat(entry.getStatus()).isEqualTo(QueueStatus.WAITING);
        assertThat(entry.getSequence()).isEqualTo(10L);
        assertThat(entry.getEnteredAt()).isEqualTo(now);
        assertThat(entry.getActivatedAt()).isNull();
        assertThat(entry.getExpiresAt()).isNull();
        assertThat(entry.getLastUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("ACTIVE 상태 hash 를 QueueEntry 로 복원한다")
    void fromHash_active() {
        Instant enteredAt = Instant.parse("2026-04-04T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-04-04T10:01:00Z");
        Instant expiresAt = Instant.parse("2026-04-04T10:06:00Z");

        Map<Object, Object> hash = new HashMap<>();
        hash.put("token", "qt_token_1");
        hash.put("queueId", "product:100");
        hash.put("userId", "1");
        hash.put("status", "ACTIVE");
        hash.put("sequence", "10");
        hash.put("enteredAt", String.valueOf(enteredAt.toEpochMilli()));
        hash.put("activatedAt", String.valueOf(activatedAt.toEpochMilli()));
        hash.put("expiresAt", String.valueOf(expiresAt.toEpochMilli()));
        hash.put("lastUpdatedAt", String.valueOf(activatedAt.toEpochMilli()));

        QueueEntry entry = mapper.fromHash(hash);

        assertThat(entry.getToken()).isEqualTo("qt_token_1");
        assertThat(entry.getQueueId()).isEqualTo("product:100");
        assertThat(entry.getUserId()).isEqualTo(1L);
        assertThat(entry.getStatus()).isEqualTo(QueueStatus.ACTIVE);
        assertThat(entry.getSequence()).isEqualTo(10L);
        assertThat(entry.getEnteredAt()).isEqualTo(enteredAt);
        assertThat(entry.getActivatedAt()).isEqualTo(activatedAt);
        assertThat(entry.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(entry.getLastUpdatedAt()).isEqualTo(activatedAt);
    }

    @Test
    @DisplayName("QueueEntry -> hash -> QueueEntry round-trip 이 가능하다")
    void roundTrip() {
        Instant enteredAt = Instant.parse("2026-04-04T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-04-04T10:01:00Z");
        Instant expiresAt = Instant.parse("2026-04-04T10:06:00Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, enteredAt);
        entry.activate(activatedAt, expiresAt);

        Map<String, String> hash = mapper.toHash(entry);
        Map<Object, Object> source = new HashMap<>(hash);

        QueueEntry restored = mapper.fromHash(source);

        assertThat(restored.getToken()).isEqualTo(entry.getToken());
        assertThat(restored.getQueueId()).isEqualTo(entry.getQueueId());
        assertThat(restored.getUserId()).isEqualTo(entry.getUserId());
        assertThat(restored.getStatus()).isEqualTo(entry.getStatus());
        assertThat(restored.getSequence()).isEqualTo(entry.getSequence());
        assertThat(restored.getEnteredAt()).isEqualTo(entry.getEnteredAt());
        assertThat(restored.getActivatedAt()).isEqualTo(entry.getActivatedAt());
        assertThat(restored.getExpiresAt()).isEqualTo(entry.getExpiresAt());
        assertThat(restored.getLastUpdatedAt()).isEqualTo(entry.getLastUpdatedAt());
    }

    @Test
    @DisplayName("필수 필드가 없으면 예외가 발생한다")
    void fromHash_fail_whenRequiredFieldMissing() {
        Map<Object, Object> hash = new HashMap<>();
        hash.put("queueId", "product:100");

        assertThatThrownBy(() -> mapper.fromHash(hash))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing required hash field");
    }

    @Test
    @DisplayName("빈 hash 이면 예외가 발생한다")
    void fromHash_fail_whenSourceEmpty() {
        assertThatThrownBy(() -> mapper.fromHash(new HashMap<>()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("source hash is empty");
    }
}