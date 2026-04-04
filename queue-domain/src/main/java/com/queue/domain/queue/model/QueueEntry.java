package com.queue.domain.queue.model;

import java.time.Instant;
import java.util.Objects;

public class QueueEntry {

    private final String token;
    private final String queueId;
    private final Long userId;

    private QueueStatus status;
    private Long sequence;

    private Instant enteredAt;
    private Instant activatedAt;
    private Instant expiresAt;
    private Instant lastUpdatedAt;

    private QueueEntry(
            String token,
            String queueId,
            Long userId,
            QueueStatus status,
            Long sequence,
            Instant enteredAt,
            Instant activatedAt,
            Instant expiresAt,
            Instant lastUpdatedAt
    ) {
        validateIdentity(token, queueId, userId);
        validateState(status, sequence, enteredAt, activatedAt, expiresAt, lastUpdatedAt);

        this.token = token;
        this.queueId = queueId;
        this.userId = userId;
        this.status = status;
        this.sequence = sequence;
        this.enteredAt = enteredAt;
        this.activatedAt = activatedAt;
        this.expiresAt = expiresAt;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public static QueueEntry enter(String token, String queueId, Long userId, Long sequence, Instant now) {
        return new QueueEntry(
                token,
                queueId,
                userId,
                QueueStatus.WAITING,
                sequence,
                now,
                null,
                null,
                now
        );
    }

    public static QueueEntry restore(
            String token,
            String queueId,
            Long userId,
            QueueStatus status,
            Long sequence,
            Instant enteredAt,
            Instant activatedAt,
            Instant expiresAt,
            Instant lastUpdatedAt
    ) {
        return new QueueEntry(
                token,
                queueId,
                userId,
                status,
                sequence,
                enteredAt,
                activatedAt,
                expiresAt,
                lastUpdatedAt
        );
    }

    public void activate(Instant now, Instant newExpiresAt) {
        if (this.status != QueueStatus.WAITING) {
            throw new IllegalStateException("WAITING 상태만 ACTIVE 로 전환할 수 있습니다.");
        }
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(newExpiresAt, "expiresAt must not be null");

        if (!newExpiresAt.isAfter(now)) {
            throw new IllegalArgumentException("expiresAt must be after now");
        }

        this.status = QueueStatus.ACTIVE;
        this.activatedAt = now;
        this.expiresAt = newExpiresAt;
        this.lastUpdatedAt = now;
    }

    public void expire(Instant now) {
        if (this.status != QueueStatus.ACTIVE) {
            throw new IllegalStateException("ACTIVE 상태만 EXPIRED 로 전환할 수 있습니다.");
        }
        Objects.requireNonNull(now, "now must not be null");

        this.status = QueueStatus.EXPIRED;
        this.lastUpdatedAt = now;
    }

    public void cancel(Instant now) {
        if (isTerminal()) {
            throw new IllegalStateException("이미 종료된 대기열 엔트리입니다.");
        }
        Objects.requireNonNull(now, "now must not be null");

        this.status = QueueStatus.CANCELLED;
        this.lastUpdatedAt = now;
    }

    public boolean isWaiting() {
        return this.status == QueueStatus.WAITING;
    }

    public boolean isActive() {
        return this.status == QueueStatus.ACTIVE;
    }

    public boolean isTerminal() {
        return this.status == QueueStatus.EXPIRED || this.status == QueueStatus.CANCELLED;
    }

    public boolean isExpiredAt(Instant now) {
        return this.expiresAt != null && !this.expiresAt.isAfter(now);
    }

    private static void validateIdentity(String token, String queueId, Long userId) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        if (queueId == null || queueId.isBlank()) {
            throw new IllegalArgumentException("queueId must not be blank");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
    }

    private static void validateState(
            QueueStatus status,
            Long sequence,
            Instant enteredAt,
            Instant activatedAt,
            Instant expiresAt,
            Instant lastUpdatedAt
    ) {
        Objects.requireNonNull(status, "status must not be null");

        if (sequence == null || sequence <= 0) {
            throw new IllegalArgumentException("sequence must be positive");
        }
        if (enteredAt == null) {
            throw new IllegalArgumentException("enteredAt must not be null");
        }
        if (lastUpdatedAt == null) {
            throw new IllegalArgumentException("lastUpdatedAt must not be null");
        }

        if ((status == QueueStatus.ACTIVE || status == QueueStatus.EXPIRED)) {
            if (activatedAt == null) {
                throw new IllegalArgumentException("activatedAt must not be null when status is ACTIVE or EXPIRED");
            }
            if (expiresAt == null) {
                throw new IllegalArgumentException("expiresAt must not be null when status is ACTIVE or EXPIRED");
            }
        }

        if (activatedAt != null && enteredAt.isAfter(activatedAt)) {
            throw new IllegalArgumentException("enteredAt must be before or equal to activatedAt");
        }

        if (activatedAt != null && expiresAt != null && !expiresAt.isAfter(activatedAt)) {
            throw new IllegalArgumentException("expiresAt must be after activatedAt");
        }
    }

    public String getToken() {
        return token;
    }

    public String getQueueId() {
        return queueId;
    }

    public Long getUserId() {
        return userId;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public Long getSequence() {
        return sequence;
    }

    public Instant getEnteredAt() {
        return enteredAt;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}