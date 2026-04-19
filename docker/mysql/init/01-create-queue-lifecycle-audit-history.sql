CREATE TABLE IF NOT EXISTS queue_lifecycle_audit_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    queue_token VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    sequence BIGINT NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    reason VARCHAR(255) NULL,
    received_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_queue_lifecycle_audit_event_id (event_id),
    KEY idx_queue_lifecycle_audit_occurred_at (occurred_at),
    KEY idx_queue_lifecycle_audit_queue_token (queue_token)
);
