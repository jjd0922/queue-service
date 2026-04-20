package com.queue.infrastructure.queue.kafka.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueueLifecycleAuditJdbcManagementAdapterTest {

    private JdbcTemplate jdbcTemplate;
    private QueueLifecycleAuditJdbcManagementAdapter adapter;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        adapter = new QueueLifecycleAuditJdbcManagementAdapter(jdbcTemplate);
    }

    @Test
    void deleteOlderThan_executesDeleteWithLimit() {
        when(jdbcTemplate.update(anyString(), any(), anyInt())).thenReturn(42);

        int deleted = adapter.deleteOlderThan(Instant.parse("2026-03-01T00:00:00Z"), 500);

        assertThat(deleted).isEqualTo(42);
        verify(jdbcTemplate).update(anyString(), any(), anyInt());
    }
}
