package com.queue.domain.model;

import com.queue.domain.exception.InvalidQueueEntryStateException;
import com.queue.domain.exception.TerminalQueueEntryException;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

@Getter
public class QueueEntry {

    private final String token;
    private final String queueId;
    private final Long userId;

    private QueueEntryStatus status;
    private final Long sequence;

    private final Instant enteredAt;
    private Instant activatedAt;
    private Instant expiresAt;
    private Instant lastUpdatedAt;

    private QueueEntry(
            String token,
            String queueId,
            Long userId,
            QueueEntryStatus status,
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
                QueueEntryStatus.WAITING,
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
            QueueEntryStatus status,
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
        if (this.status != QueueEntryStatus.WAITING) {
            throw new InvalidQueueEntryStateException();
        }
        Objects.requireNonNull(now, "현재 시간은 필수입니다.");
        Objects.requireNonNull(newExpiresAt, "만료 시간은 필수입니다.");

        if (!newExpiresAt.isAfter(now)) {
            throw new IllegalArgumentException("만료 시간은 현재 시간보다 이후여야 합니다.");
        }

        this.status = QueueEntryStatus.ACTIVE;
        this.activatedAt = now;
        this.expiresAt = newExpiresAt;
        this.lastUpdatedAt = now;
    }

    public void expire(Instant now) {
        if (this.status != QueueEntryStatus.ACTIVE) {
            throw new InvalidQueueEntryStateException();
        }
        Objects.requireNonNull(now, "현재 시간은 필수입니다.");

        this.status = QueueEntryStatus.EXPIRED;
        this.lastUpdatedAt = now;
    }

    public void cancel(Instant now) {
        if (isTerminal()) {
            throw new TerminalQueueEntryException();
        }
        Objects.requireNonNull(now, "현재 시간은 필수입니다.");

        this.status = QueueEntryStatus.CANCELLED;
        this.lastUpdatedAt = now;
    }

    public boolean isWaiting() {
        return this.status == QueueEntryStatus.WAITING;
    }

    public boolean isActive() {
        return this.status == QueueEntryStatus.ACTIVE;
    }

    public boolean isTerminal() {
        return this.status == QueueEntryStatus.EXPIRED || this.status == QueueEntryStatus.CANCELLED;
    }

    public boolean isExpiredAt(Instant now) {
        return this.expiresAt != null && !this.expiresAt.isAfter(now);
    }

    private static void validateIdentity(String token, String queueId, Long userId) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("토큰은 비어 있을 수 없습니다.");
        }
        if (queueId == null || queueId.isBlank()) {
            throw new IllegalArgumentException("대기열 ID는 비어 있을 수 없습니다.");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("사용자 ID는 0보다 커야 합니다.");
        }
    }

    private static void validateState(
            QueueEntryStatus status,
            Long sequence,
            Instant enteredAt,
            Instant activatedAt,
            Instant expiresAt,
            Instant lastUpdatedAt
    ) {
        Objects.requireNonNull(status, "대기열 상태는 필수입니다.");

        if (sequence == null || sequence <= 0) {
            throw new IllegalArgumentException("순번은 0보다 커야 합니다.");
        }
        if (enteredAt == null) {
            throw new IllegalArgumentException("진입 시간은 필수입니다.");
        }
        if (lastUpdatedAt == null) {
            throw new IllegalArgumentException("최종 수정 시간은 필수입니다.");
        }

        if (status == QueueEntryStatus.ACTIVE || status == QueueEntryStatus.EXPIRED) {
            if (activatedAt == null) {
                throw new IllegalArgumentException("ACTIVE 또는 EXPIRED 상태에서는 활성화 시간이 필수입니다.");
            }
            if (expiresAt == null) {
                throw new IllegalArgumentException("ACTIVE 또는 EXPIRED 상태에서는 만료 시간이 필수입니다.");
            }
        }

        if (activatedAt != null && enteredAt.isAfter(activatedAt)) {
            throw new IllegalArgumentException("진입 시간은 활성화 시간보다 늦을 수 없습니다.");
        }

        if (activatedAt != null && expiresAt != null && !expiresAt.isAfter(activatedAt)) {
            throw new IllegalArgumentException("만료 시간은 활성화 시간보다 이후여야 합니다.");
        }
    }
}