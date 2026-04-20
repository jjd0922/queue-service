package com.queue.infrastructure.queue.kafka.adapter;

import com.queue.application.dto.command.RecordQueueLifecycleAuditCommand;
import com.queue.application.port.out.QueueLifecycleAuditCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueueLifecycleAuditJdbcAdapter implements QueueLifecycleAuditCommandPort {

    private static final String INSERT_SQL = """
            INSERT INTO queue_lifecycle_audit_history
            (event_id, event_type, queue_token, user_id, status, sequence, occurred_at, reason, received_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean insertIfAbsent(RecordQueueLifecycleAuditCommand command) {
        try {
            int updated = jdbcTemplate.update(
                    INSERT_SQL,
                    command.eventId(),
                    command.eventType(),
                    command.queueToken(),
                    command.userId(),
                    command.status(),
                    command.sequence(),
                    command.occurredAt(),
                    command.reason(),
                    command.receivedAt()
            );
            return updated > 0;
        } catch (DuplicateKeyException ignored) {
            return false;
        }
    }
}
