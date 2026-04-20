package com.queue.application.service;

import com.queue.application.dto.command.EnqueueCommand;
import com.queue.application.dto.command.EnterQueueCommand;
import com.queue.application.dto.result.EnterQueueResult;
import com.queue.application.port.out.QueueEnqueueCommandPort;
import com.queue.application.port.out.QueueLifecycleEventPort;
import com.queue.application.port.out.QueueQueryPort;
import com.queue.domain.event.QueueLifecycleEvent;
import com.queue.domain.model.EnqueueDecision;
import com.queue.domain.model.EnqueueOutcome;
import com.queue.domain.model.QueueEntry;
import com.queue.domain.model.QueueEntryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnterQueueServiceTest {

    @Mock
    private QueueEnqueueCommandPort queueEnqueueCommandPort;

    @Mock
    private QueueQueryPort queueQueryPort;

    @Mock
    private QueueLifecycleEventPort queueLifecycleEventPort;

    private Clock clock;

    private EnterQueueService enterQueueService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-04-05T00:00:00Z"), ZoneOffset.UTC);
        enterQueueService = new EnterQueueService(
                queueEnqueueCommandPort,
                queueQueryPort,
                queueLifecycleEventPort,
                clock
        );
    }

    @Test
    @DisplayName("신규 WAITING 엔트리 생성 시 순번을 조회해 반환하고 lifecycle event를 발행한다")
    void enter_whenCreatedWaitingEntry_thenReturnPositionFromRank() {
        EnterQueueCommand command = new EnterQueueCommand("queue-1", 1L);

        QueueEntry entry = QueueEntry.enter(
                "token-1",
                "queue-1",
                1L,
                1L,
                Instant.parse("2026-04-05T00:00:00Z")
        );

        given(queueEnqueueCommandPort.enqueueOrGetExisting(any(EnqueueCommand.class)))
                .willReturn(new EnqueueDecision(EnqueueOutcome.CREATED, entry));
        given(queueQueryPort.findRank("queue-1", "token-1"))
                .willReturn(1L);

        EnterQueueResult result = enterQueueService.enter(command);

        assertThat(result.token()).isEqualTo("token-1");
        assertThat(result.queueId()).isEqualTo("queue-1");
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo("WAITING");
        assertThat(result.position()).isEqualTo(1L);
        assertThat(result.enteredAt()).isEqualTo(Instant.parse("2026-04-05T00:00:00Z"));
        assertThat(result.expiresAt()).isNull();

        verify(queueEnqueueCommandPort).enqueueOrGetExisting(
                new EnqueueCommand("queue-1", 1L, Instant.parse("2026-04-05T00:00:00Z"))
        );
        verify(queueQueryPort).findRank("queue-1", "token-1");
        verify(queueLifecycleEventPort).publish(any(QueueLifecycleEvent.class));
    }

    @Test
    @DisplayName("이미 WAITING 상태인 엔트리가 있으면 기존 엔트리와 순번을 반환하고 lifecycle event는 발행하지 않는다")
    void enter_whenAlreadyWaiting_thenReturnExistingEntryAndPosition() {
        EnterQueueCommand command = new EnterQueueCommand("queue-1", 1L);

        QueueEntry entry = QueueEntry.enter(
                "token-1",
                "queue-1",
                1L,
                1L,
                Instant.parse("2026-04-05T00:00:00Z")
        );

        given(queueEnqueueCommandPort.enqueueOrGetExisting(any(EnqueueCommand.class)))
                .willReturn(new EnqueueDecision(EnqueueOutcome.ALREADY_WAITING, entry));
        given(queueQueryPort.findRank("queue-1", "token-1"))
                .willReturn(3L);

        EnterQueueResult result = enterQueueService.enter(command);

        assertThat(result.token()).isEqualTo("token-1");
        assertThat(result.status()).isEqualTo("WAITING");
        assertThat(result.position()).isEqualTo(3L);
        assertThat(result.expiresAt()).isNull();

        verify(queueQueryPort).findRank("queue-1", "token-1");
        verify(queueLifecycleEventPort, never()).publish(any(QueueLifecycleEvent.class));
    }

    @Test
    @DisplayName("이미 ACTIVE 상태인 엔트리가 있으면 순번 0으로 반환하고 lifecycle event는 발행하지 않는다")
    void enter_whenAlreadyActive_thenReturnPositionZero() {
        EnterQueueCommand command = new EnterQueueCommand("queue-1", 1L);

        QueueEntry activeEntry = QueueEntry.restore(
                "token-1",
                "queue-1",
                1L,
                QueueEntryStatus.ACTIVE,
                1L,
                Instant.parse("2026-04-05T00:00:00Z"),
                Instant.parse("2026-04-05T00:01:00Z"),
                Instant.parse("2026-04-05T00:11:00Z"),
                Instant.parse("2026-04-05T00:01:00Z")
        );

        given(queueEnqueueCommandPort.enqueueOrGetExisting(any(EnqueueCommand.class)))
                .willReturn(new EnqueueDecision(EnqueueOutcome.ALREADY_ACTIVE, activeEntry));

        EnterQueueResult result = enterQueueService.enter(command);

        assertThat(result.token()).isEqualTo("token-1");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.position()).isEqualTo(0L);
        assertThat(result.expiresAt()).isEqualTo(Instant.parse("2026-04-05T00:11:00Z"));
        verify(queueLifecycleEventPort, never()).publish(any(QueueLifecycleEvent.class));
    }

    @Test
    @DisplayName("WAITING 상태이지만 순번 조회 결과가 없으면 position은 null로 반환하고 lifecycle event는 발행한다")
    void enter_whenWaitingButRankMissing_thenReturnNullPosition() {
        EnterQueueCommand command = new EnterQueueCommand("queue-1", 1L);

        QueueEntry entry = QueueEntry.enter(
                "token-1",
                "queue-1",
                1L,
                1L,
                Instant.parse("2026-04-05T00:00:00Z")
        );

        given(queueEnqueueCommandPort.enqueueOrGetExisting(any(EnqueueCommand.class)))
                .willReturn(new EnqueueDecision(EnqueueOutcome.CREATED, entry));
        given(queueQueryPort.findRank("queue-1", "token-1"))
                .willReturn(null);

        EnterQueueResult result = enterQueueService.enter(command);

        assertThat(result.status()).isEqualTo("WAITING");
        assertThat(result.position()).isNull();
        verify(queueLifecycleEventPort).publish(any(QueueLifecycleEvent.class));
    }
}