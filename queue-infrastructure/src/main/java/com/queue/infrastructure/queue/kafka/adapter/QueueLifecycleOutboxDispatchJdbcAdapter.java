package com.queue.infrastructure.queue.kafka.adapter;

import com.queue.infrastructure.queue.kafka.outbox.QueueLifecycleOutboxEvent;
import com.queue.infrastructure.queue.kafka.outbox.QueueLifecycleOutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class QueueLifecycleOutboxDispatchJdbcAdapter {

    private static final String FIND_DISPATCHABLE_SQL = """
            SELECT id, event_id, event_type, queue_token, user_id, status, sequence,
                   occurred_at, reason, payload, retry_count
            FROM queue_lifecycle_outbox
            WHERE publish_status IN (?, ?)
              AND next_retry_at <= ?
            ORDER BY id ASC
            LIMIT ?
            """;

    private static final String CLAIM_SQL = """
            UPDATE queue_lifecycle_outbox
            SET publish_status = ?
            WHERE id = ?
              AND publish_status IN (?, ?)
            """;

    private static final String MARK_PUBLISHED_SQL = """
            UPDATE queue_lifecycle_outbox
            SET publish_status = ?,
                published_at = ?,
                last_error_message = NULL
            WHERE id = ?
            """;

    private static final String MARK_FAILED_SQL = """
            UPDATE queue_lifecycle_outbox
            SET publish_status = ?,
                retry_count = ?,
                next_retry_at = ?,
                last_error_message = ?
            WHERE id = ?
            """;

    private static final RowMapper<QueueLifecycleOutboxEvent> ROW_MAPPER =
            (rs, rowNum) -> new QueueLifecycleOutboxEvent(
                    rs.getLong("id"),
                    rs.getString("event_id"),
                    rs.getString("event_type"),
                    rs.getString("queue_token"),
                    rs.getLong("user_id"),
                    rs.getString("status"),
                    rs.getLong("sequence"),
                    rs.getTimestamp("occurred_at").toInstant(),
                    rs.getString("reason"),
                    rs.getString("payload"),
                    rs.getInt("retry_count")
            );

    private final JdbcTemplate jdbcTemplate;

    public List<QueueLifecycleOutboxEvent> findDispatchable(int batchSize, Instant now) {
        return jdbcTemplate.query(
                FIND_DISPATCHABLE_SQL,
                ROW_MAPPER,
                QueueLifecycleOutboxStatus.PENDING.name(),
                QueueLifecycleOutboxStatus.FAILED.name(),
                now,
                batchSize
        );
    }

    public boolean claim(Long id) {
        int updated = jdbcTemplate.update(
                CLAIM_SQL,
                QueueLifecycleOutboxStatus.PROCESSING.name(),
                id,
                QueueLifecycleOutboxStatus.PENDING.name(),
                QueueLifecycleOutboxStatus.FAILED.name()
        );
        return updated > 0;
    }

    public void markPublished(Long id, Instant publishedAt) {
        jdbcTemplate.update(
                MARK_PUBLISHED_SQL,
                QueueLifecycleOutboxStatus.PUBLISHED.name(),
                publishedAt,
                id
        );
    }

    public QueueLifecycleOutboxStatus markFailed(
            QueueLifecycleOutboxEvent event,
            Instant failedAt,
            int maxRetryCount,
            long retryBackoffMs,
            String errorMessage
    ) {
        int nextRetryCount = event.retryCount() + 1;
        QueueLifecycleOutboxStatus nextStatus = nextRetryCount >= maxRetryCount
                ? QueueLifecycleOutboxStatus.DEAD
                : QueueLifecycleOutboxStatus.FAILED;
        Instant nextRetryAt = nextStatus == QueueLifecycleOutboxStatus.DEAD
                ? failedAt
                : failedAt.plusMillis(Math.max(retryBackoffMs, 0L));

        jdbcTemplate.update(
                MARK_FAILED_SQL,
                nextStatus.name(),
                nextRetryCount,
                nextRetryAt,
                abbreviate(errorMessage),
                event.id()
        );
        return nextStatus;
    }

    private String abbreviate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }
}
