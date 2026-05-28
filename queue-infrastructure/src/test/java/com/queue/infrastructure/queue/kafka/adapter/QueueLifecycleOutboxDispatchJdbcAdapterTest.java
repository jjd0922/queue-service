package com.queue.infrastructure.queue.kafka.adapter;

import com.queue.infrastructure.queue.kafka.outbox.QueueLifecycleOutboxEvent;
import com.queue.infrastructure.queue.kafka.outbox.QueueLifecycleOutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueueLifecycleOutboxDispatchJdbcAdapterTest {

    private JdbcTemplate jdbcTemplate;
    private QueueLifecycleOutboxDispatchJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        adapter = new QueueLifecycleOutboxDispatchJdbcAdapter(jdbcTemplate);
    }

    @Test
    void claim_returnsTrueWhenRowUpdated() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any())).thenReturn(1);

        boolean claimed = adapter.claim(1L);

        assertThat(claimed).isTrue();
    }

    @Test
    void claim_returnsFalseWhenRowNotUpdated() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any())).thenReturn(0);

        boolean claimed = adapter.claim(1L);

        assertThat(claimed).isFalse();
    }

    @Test
    void markFailed_returnsFailedWhenRetryRemains() {
        QueueLifecycleOutboxStatus status = adapter.markFailed(
                event(1),
                Instant.parse("2026-04-18T01:00:00Z"),
                3,
                1000L,
                "kafka down"
        );

        assertThat(status).isEqualTo(QueueLifecycleOutboxStatus.FAILED);
        verify(jdbcTemplate).update(
                anyString(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void markFailed_returnsDeadWhenRetryExceeded() {
        QueueLifecycleOutboxStatus status = adapter.markFailed(
                event(2),
                Instant.parse("2026-04-18T01:00:00Z"),
                3,
                1000L,
                "kafka down"
        );

        assertThat(status).isEqualTo(QueueLifecycleOutboxStatus.DEAD);
    }

    private QueueLifecycleOutboxEvent event(int retryCount) {
        return new QueueLifecycleOutboxEvent(
                1L,
                "event-1",
                "ENTERED",
                "token-1",
                1L,
                "WAITING",
                1L,
                Instant.parse("2026-04-18T01:00:00Z"),
                null,
                "{}",
                retryCount
        );
    }
}
