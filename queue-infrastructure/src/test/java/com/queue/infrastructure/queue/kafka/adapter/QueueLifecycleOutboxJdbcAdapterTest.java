package com.queue.infrastructure.queue.kafka.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.queue.domain.event.QueueLifecycleEvent;
import com.queue.domain.model.QueueEntryStatus;
import com.queue.infrastructure.queue.kafka.mapper.QueueLifecycleEventMessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueueLifecycleOutboxJdbcAdapterTest {

    private JdbcTemplate jdbcTemplate;
    private QueueLifecycleOutboxJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        adapter = new QueueLifecycleOutboxJdbcAdapter(
                jdbcTemplate,
                objectMapper,
                new QueueLifecycleEventMessageMapper()
        );
    }

    @Test
    void publish_insertsPendingOutboxEvent() {
        QueueLifecycleEvent event = QueueLifecycleEvent.entered(
                "token-1",
                1L,
                QueueEntryStatus.WAITING,
                1L,
                Instant.parse("2026-04-18T01:00:00Z")
        );

        adapter.publish(event);

        verify(jdbcTemplate).update(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                any(),
                anyString(),
                any(),
                any(),
                any(),
                anyString(),
                anyString(),
                anyInt(),
                any()
        );
    }

    @Test
    void publish_ignoresDuplicateEvent() {
        when(jdbcTemplate.update(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                any(),
                anyString(),
                any(),
                any(),
                any(),
                anyString(),
                anyString(),
                anyInt(),
                any()
        )).thenThrow(new DuplicateKeyException("duplicate"));

        QueueLifecycleEvent event = QueueLifecycleEvent.entered(
                "token-1",
                1L,
                QueueEntryStatus.WAITING,
                1L,
                Instant.parse("2026-04-18T01:00:00Z")
        );

        assertThatCode(() -> adapter.publish(event)).doesNotThrowAnyException();
    }
}
