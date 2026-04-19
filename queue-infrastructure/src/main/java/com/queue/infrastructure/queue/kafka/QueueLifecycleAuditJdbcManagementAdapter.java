package com.queue.infrastructure.queue.kafka;

import com.queue.application.port.out.QueueLifecycleAuditManagementPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class QueueLifecycleAuditJdbcManagementAdapter implements QueueLifecycleAuditManagementPort {

    private static final String DELETE_SQL = """
            DELETE FROM queue_lifecycle_audit_history
            WHERE created_at < ?
            ORDER BY created_at
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int deleteOlderThan(Instant cutoff, int batchSize) {
        return jdbcTemplate.update(
                DELETE_SQL,
                Timestamp.from(cutoff),
                batchSize
        );
    }
}
