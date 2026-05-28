package com.queue.infrastructure.queue.kafka.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.queue.application.port.out.QueueLifecycleEventPort;
import com.queue.domain.event.QueueLifecycleEvent;
import com.queue.infrastructure.queue.kafka.mapper.QueueLifecycleEventMessageMapper;
import com.queue.infrastructure.queue.kafka.model.QueueLifecycleEventMessage;
import com.queue.infrastructure.queue.kafka.outbox.QueueLifecycleOutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueLifecycleOutboxJdbcAdapter implements QueueLifecycleEventPort {

    private static final String INSERT_SQL = """
            INSERT INTO queue_lifecycle_outbox
            (event_id, event_type, queue_token, user_id, status, sequence, occurred_at, reason,
             payload, publish_status, retry_count, next_retry_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final QueueLifecycleEventMessageMapper mapper;

    @Override
    public void publish(QueueLifecycleEvent event) {
        QueueLifecycleEventMessage message = mapper.map(event);
        String payload = serialize(message);

        try {
            jdbcTemplate.update(
                    INSERT_SQL,
                    event.getEventId(),
                    event.getType().name(),
                    event.getQueueToken(),
                    event.getUserId(),
                    event.getStatus().name(),
                    event.getSequence(),
                    event.getOccurredAt(),
                    event.getReason(),
                    payload,
                    QueueLifecycleOutboxStatus.PENDING.name(),
                    0,
                    event.getOccurredAt()
            );
        } catch (DuplicateKeyException e) {
            log.info(
                    "queue lifecycle outbox duplicate ignored. eventId={}, eventType={}, queueToken={}",
                    event.getEventId(),
                    event.getType(),
                    event.getQueueToken()
            );
        }
    }

    private String serialize(QueueLifecycleEventMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize queue lifecycle outbox payload.", e);
        }
    }
}
