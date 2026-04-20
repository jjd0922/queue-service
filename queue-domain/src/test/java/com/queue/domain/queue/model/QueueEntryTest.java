package com.queue.domain.queue.model;

import com.queue.domain.exception.InvalidQueueEntryStateException;
import com.queue.domain.exception.TerminalQueueEntryException;
import com.queue.domain.model.QueueEntry;
import com.queue.domain.model.QueueEntryStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueueEntryTest {

    @Test
    @DisplayName("입장 시 WAITING 상태의 엔트리를 생성한다")
    void enter_success() {
        Instant now = Instant.parse("2026-04-04T10:00:00Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, now);

        assertThat(entry.getToken()).isEqualTo("qt_token_1");
        assertThat(entry.getQueueId()).isEqualTo("product:100");
        assertThat(entry.getUserId()).isEqualTo(1L);
        assertThat(entry.getStatus()).isEqualTo(QueueEntryStatus.WAITING);
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
    @DisplayName("WAITING 상태의 엔트리를 ACTIVE 상태로 전환한다")
    void activate_success() {
        Instant enteredAt = Instant.parse("2026-04-04T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-04-04T10:01:00Z");
        Instant expiresAt = Instant.parse("2026-04-04T10:06:00Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, enteredAt);

        entry.activate(activatedAt, expiresAt);

        assertThat(entry.getStatus()).isEqualTo(QueueEntryStatus.ACTIVE);
        assertThat(entry.getActivatedAt()).isEqualTo(activatedAt);
        assertThat(entry.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(entry.getLastUpdatedAt()).isEqualTo(activatedAt);
        assertThat(entry.isWaiting()).isFalse();
        assertThat(entry.isActive()).isTrue();
        assertThat(entry.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("WAITING 상태가 아니면 ACTIVE 상태로 전환할 수 없다")
    void activate_fail_whenStatusIsNotWaiting() {
        Instant enteredAt = Instant.parse("2026-04-04T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-04-04T10:01:00Z");
        Instant expiresAt = Instant.parse("2026-04-04T10:06:00Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, enteredAt);
        entry.activate(activatedAt, expiresAt);

        assertThatThrownBy(() -> entry.activate(activatedAt.plusSeconds(1), expiresAt.plusSeconds(1)))
                .isInstanceOf(InvalidQueueEntryStateException.class)
                .hasMessage("허용되지 않은 대기열 상태 전이입니다.");
    }

    @Test
    @DisplayName("만료 시각이 현재 시각 이후가 아니면 ACTIVE 상태로 전환할 수 없다")
    void activate_fail_whenExpiresAtIsNotAfterNow() {
        Instant enteredAt = Instant.parse("2026-04-04T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-04-04T10:01:00Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, enteredAt);

        assertThatThrownBy(() -> entry.activate(activatedAt, activatedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("만료 시각은 현재 시각보다 이후여야 합니다.");
    }

    @Test
    @DisplayName("ACTIVE 상태의 엔트리를 EXPIRED 상태로 전환한다")
    void expire_success() {
        Instant enteredAt = Instant.parse("2026-04-04T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-04-04T10:01:00Z");
        Instant expiresAt = Instant.parse("2026-04-04T10:06:00Z");
        Instant expiredAt = Instant.parse("2026-04-04T10:06:01Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, enteredAt);
        entry.activate(activatedAt, expiresAt);

        entry.expire(expiredAt);

        assertThat(entry.getStatus()).isEqualTo(QueueEntryStatus.EXPIRED);
        assertThat(entry.getLastUpdatedAt()).isEqualTo(expiredAt);
        assertThat(entry.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("ACTIVE 상태가 아니면 EXPIRED 상태로 전환할 수 없다")
    void expire_fail_whenStatusIsNotActive() {
        Instant now = Instant.parse("2026-04-04T10:00:00Z");
        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, now);

        assertThatThrownBy(() -> entry.expire(now.plusSeconds(60)))
                .isInstanceOf(InvalidQueueEntryStateException.class)
                .hasMessage("허용되지 않은 대기열 상태 전이입니다.");
    }

    @Test
    @DisplayName("WAITING 상태의 엔트리를 CANCELLED 상태로 전환한다")
    void cancel_success_whenWaiting() {
        Instant now = Instant.parse("2026-04-04T10:00:00Z");
        Instant cancelledAt = Instant.parse("2026-04-04T10:02:00Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, now);
        entry.cancel(cancelledAt);

        assertThat(entry.getStatus()).isEqualTo(QueueEntryStatus.CANCELLED);
        assertThat(entry.getLastUpdatedAt()).isEqualTo(cancelledAt);
        assertThat(entry.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("ACTIVE 상태의 엔트리를 CANCELLED 상태로 전환한다")
    void cancel_success_whenActive() {
        Instant enteredAt = Instant.parse("2026-04-04T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-04-04T10:01:00Z");
        Instant expiresAt = Instant.parse("2026-04-04T10:06:00Z");
        Instant cancelledAt = Instant.parse("2026-04-04T10:03:00Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, enteredAt);
        entry.activate(activatedAt, expiresAt);
        entry.cancel(cancelledAt);

        assertThat(entry.getStatus()).isEqualTo(QueueEntryStatus.CANCELLED);
        assertThat(entry.getLastUpdatedAt()).isEqualTo(cancelledAt);
        assertThat(entry.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("이미 종료 상태이면 CANCELLED 상태로 전환할 수 없다")
    void cancel_fail_whenAlreadyTerminal() {
        Instant enteredAt = Instant.parse("2026-04-04T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-04-04T10:01:00Z");
        Instant expiresAt = Instant.parse("2026-04-04T10:06:00Z");
        Instant expiredAt = Instant.parse("2026-04-04T10:06:01Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, enteredAt);
        entry.activate(activatedAt, expiresAt);
        entry.expire(expiredAt);

        assertThatThrownBy(() -> entry.cancel(expiredAt.plusSeconds(1)))
                .isInstanceOf(TerminalQueueEntryException.class)
                .hasMessage("이미 종료된 대기열 엔트리입니다.");
    }

    @Test
    @DisplayName("현재 시각이 만료 시각과 같거나 이후이면 만료 상태로 판단한다")
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
    @DisplayName("현재 시각이 만료 시각 이전이면 만료 상태가 아니다")
    void isExpiredAt_false() {
        Instant enteredAt = Instant.parse("2026-04-04T10:00:00Z");
        Instant activatedAt = Instant.parse("2026-04-04T10:01:00Z");
        Instant expiresAt = Instant.parse("2026-04-04T10:06:00Z");

        QueueEntry entry = QueueEntry.enter("qt_token_1", "product:100", 1L, 10L, enteredAt);
        entry.activate(activatedAt, expiresAt);

        assertThat(entry.isExpiredAt(Instant.parse("2026-04-04T10:05:59Z"))).isFalse();
    }
}