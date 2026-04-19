package com.queue.domain.queue.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueueEntryTest {

    @Test
    @DisplayName("enter - WAITING 상태의 QueueEntry를 생성한다")
    void enter_success() {
        Instant now = Instant.parse("2026-04-04T10:00:00Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, now);

        assertThat(entry.getToken()).isEqualTo("qt_token_1");
        assertThat(entry.getQueueId()).isEqualTo("product:100");
        assertThat(entry.getUserId()).isEqualTo(1L);
        assertThat(entry.getStatus()).isEqualTo(QueueStatus.WAITING);
        assertThat(entry.getSequence()).isEqualTo(10L);
        assertThat(entry.getEnteredAt()).isEqualTo(now);
        assertThat(entry.getActivatedAt()).isNull();
        assertThat(entry.getExpiresAt()).isNull();
        assertThat(entry.getLastUpdatedAt()).isEqualTo(now);
        assertThat(entry.isWaiting()).isTrue();
        assertThat(entry.isActive()).isFalse();
        assertThat(entry.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("activate - WAITING 상태를 ACTIVE 로 변경한다")
    void activate_success() {
        Instant enteredAt = Instant.parse("2026-04-04T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-04-04T10:01:00Z");
        Instant expiresAt = Instant.parse("2026-04-04T10:06:00Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, enteredAt);

        entry.activate(activatedAt, expiresAt);

        assertThat(entry.getStatus()).isEqualTo(QueueStatus.ACTIVE);
        assertThat(entry.getActivatedAt()).isEqualTo(activatedAt);
        assertThat(entry.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(entry.getLastUpdatedAt()).isEqualTo(activatedAt);
        assertThat(entry.isWaiting()).isFalse();
        assertThat(entry.isActive()).isTrue();
        assertThat(entry.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("activate - WAITING 상태가 아니면 예외가 발생한다")
    void activate_fail_whenStatusIsNotWaiting() {
        Instant enteredAt = Instant.parse("2026-04-04T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-04-04T10:01:00Z");
        Instant expiresAt = Instant.parse("2026-04-04T10:06:00Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, enteredAt);
        entry.activate(activatedAt, expiresAt);

        assertThatThrownBy(() -> entry.activate(activatedAt.plusSeconds(1), expiresAt.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("WAITING 상태만 ACTIVE 로 전환할 수 있습니다.");
    }

    @Test
    @DisplayName("activate - expiresAt 이 now 이후가 아니면 예외가 발생한다")
    void activate_fail_whenExpiresAtIsNotAfterNow() {
        Instant enteredAt = Instant.parse("2026-04-04T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-04-04T10:01:00Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, enteredAt);

        assertThatThrownBy(() -> entry.activate(activatedAt, activatedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expiresAt must be after now");
    }

    @Test
    @DisplayName("expire - ACTIVE 상태를 EXPIRED 로 변경한다")
    void expire_success() {
        Instant enteredAt = Instant.parse("2026-04-04T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-04-04T10:01:00Z");
        Instant expiresAt = Instant.parse("2026-04-04T10:06:00Z");
        Instant expiredAt = Instant.parse("2026-04-04T10:06:01Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, enteredAt);
        entry.activate(activatedAt, expiresAt);

        entry.expire(expiredAt);

        assertThat(entry.getStatus()).isEqualTo(QueueStatus.EXPIRED);
        assertThat(entry.getLastUpdatedAt()).isEqualTo(expiredAt);
        assertThat(entry.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("expire - ACTIVE 상태가 아니면 예외가 발생한다")
    void expire_fail_whenStatusIsNotActive() {
        Instant now = Instant.parse("2026-04-04T10:00:00Z");
        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, now);

        assertThatThrownBy(() -> entry.expire(now.plusSeconds(60)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ACTIVE 상태만 EXPIRED 로 전환할 수 있습니다.");
    }

    @Test
    @DisplayName("cancel - WAITING 상태를 CANCELLED 로 변경한다")
    void cancel_success_whenWaiting() {
        Instant now = Instant.parse("2026-04-04T10:00:00Z");
        Instant cancelledAt = Instant.parse("2026-04-04T10:02:00Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, now);

        entry.cancel(cancelledAt);

        assertThat(entry.getStatus()).isEqualTo(QueueStatus.CANCELLED);
        assertThat(entry.getLastUpdatedAt()).isEqualTo(cancelledAt);
        assertThat(entry.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("cancel - ACTIVE 상태를 CANCELLED 로 변경한다")
    void cancel_success_whenActive() {
        Instant enteredAt = Instant.parse("2026-04-04T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-04-04T10:01:00Z");
        Instant expiresAt = Instant.parse("2026-04-04T10:06:00Z");
        Instant cancelledAt = Instant.parse("2026-04-04T10:03:00Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, enteredAt);
        entry.activate(activatedAt, expiresAt);

        entry.cancel(cancelledAt);

        assertThat(entry.getStatus()).isEqualTo(QueueStatus.CANCELLED);
        assertThat(entry.getLastUpdatedAt()).isEqualTo(cancelledAt);
        assertThat(entry.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("cancel - terminal 상태면 예외가 발생한다")
    void cancel_fail_whenAlreadyTerminal() {
        Instant enteredAt = Instant.parse("2026-04-04T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-04-04T10:01:00Z");
        Instant expiresAt = Instant.parse("2026-04-04T10:06:00Z");
        Instant expiredAt = Instant.parse("2026-04-04T10:06:01Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, enteredAt);
        entry.activate(activatedAt, expiresAt);
        entry.expire(expiredAt);

        assertThatThrownBy(() -> entry.cancel(expiredAt.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 종료된 대기열 엔트리입니다.");
    }

    @Test
    @DisplayName("isExpiredAt - expiresAt 이 현재 시각보다 이전이거나 같으면 true")
    void isExpiredAt_true() {
        Instant enteredAt = Instant.parse("2026-04-04T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-04-04T10:01:00Z");
        Instant expiresAt = Instant.parse("2026-04-04T10:06:00Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, enteredAt);
        entry.activate(activatedAt, expiresAt);

        assertThat(entry.isExpiredAt(Instant.parse("2026-04-04T10:06:00Z"))).isTrue();
        assertThat(entry.isExpiredAt(Instant.parse("2026-04-04T10:06:01Z"))).isTrue();
    }

    @Test
    @DisplayName("isExpiredAt - expiresAt 이 현재 시각보다 이후면 false")
    void isExpiredAt_false() {
        Instant enteredAt = Instant.parse("2026-04-04T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-04-04T10:01:00Z");
        Instant expiresAt = Instant.parse("2026-04-04T10:06:00Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, enteredAt);
        entry.activate(activatedAt, expiresAt);

        assertThat(entry.isExpiredAt(Instant.parse("2026-04-04T10:05:59Z"))).isFalse();
    }
}