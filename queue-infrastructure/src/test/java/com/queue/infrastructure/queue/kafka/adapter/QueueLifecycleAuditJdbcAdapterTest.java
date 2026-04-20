package com.queue.infrastructure.queue.kafka.adapter;

import com.queue.application.dto.command.RecordQueueLifecycleAuditCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueueLifecycleAuditJdbcAdapterTest {

    private JdbcTemplate jdbcTemplate;
    private QueueLifecycleAuditJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        adapter = new QueueLifecycleAuditJdbcAdapter(jdbcTemplate);
    }

    @Test
    void insertIfAbsent_returnsTrue_whenInserted() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), anyLong(), any(), anyLong(), any(), any(), any()))
                .thenReturn(1);

        boolean inserted = adapter.insertIfAbsent(command());

        assertThat(inserted).isTrue();
    }

    @Test
    void insertIfAbsent_returnsFalse_whenDuplicateKey() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), anyLong(), any(), anyLong(), any(), any(), any()))
                .thenThrow(new DuplicateKeyException("duplicate"));

        boolean inserted = adapter.insertIfAbsent(command());

        assertThat(inserted).isFalse();
    }

    private RecordQueueLifecycleAuditCommand command() {
        return new RecordQueueLifecycleAuditCommand(
                "event-1",
                "ENTERED",
                "token-1",
                1L,
                "WAITING",
                1L,
                Instant.parse("2026-04-18T01:00:00Z"),
                null,
                Instant.parse("2026-04-18T01:00:01Z")
        );
    }
}
