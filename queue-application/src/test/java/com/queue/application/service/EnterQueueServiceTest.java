package com.queue.application.service;

import com.queue.application.dto.EnqueueCommand;
import com.queue.application.dto.EnterQueueCommand;
import com.queue.application.dto.EnterQueueResult;
import com.queue.application.port.out.QueueCommandPort;
import com.queue.application.port.out.QueueQueryPort;
import com.queue.domain.model.EnqueueDecision;
import com.queue.domain.model.EnqueueOutcome;
import com.queue.domain.model.QueueEntry;
import com.queue.domain.model.QueueEntryStatus;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnterQueueServiceTest {

    @Mock
    private QueueCommandPort queueCommandPort;

    @Mock
    private QueueQueryPort queueQueryPort;

    private Clock clock;

    private EnterQueueService enterQueueService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-04-05T00:00:00Z"), ZoneOffset.UTC);
        enterQueueService = new EnterQueueService(queueCommandPort, queueQueryPort, clock);
    }

    @Test
    void enter_whenCreatedWaitingEntry_thenReturnPositionFromRank() {
        EnterQueueCommand command = new EnterQueueCommand("queue-1", 1L);

        QueueEntry entry = QueueEntry.enter(
                "token-1",
                "queue-1",
                1L,
                1L,
                Instant.parse("2026-04-05T00:00:00Z")
        );

        given(queueCommandPort.enqueueOrGetExisting(any(EnqueueCommand.class)))
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

        verify(queueCommandPort).enqueueOrGetExisting(
                new EnqueueCommand("queue-1", 1L, Instant.parse("2026-04-05T00:00:00Z"))
        );
        verify(queueQueryPort).findRank("queue-1", "token-1");
    }

    @Test
    void enter_whenAlreadyWaiting_thenReturnExistingEntryAndPosition() {
        EnterQueueCommand command = new EnterQueueCommand("queue-1", 1L);

        QueueEntry entry = QueueEntry.enter(
                "token-1",
                "queue-1",
                1L,
                1L,
                Instant.parse("2026-04-05T00:00:00Z")
        );

        given(queueCommandPort.enqueueOrGetExisting(any(EnqueueCommand.class)))
                .willReturn(new EnqueueDecision(EnqueueOutcome.ALREADY_WAITING, entry));
        given(queueQueryPort.findRank("queue-1", "token-1"))
                .willReturn(3L);

        EnterQueueResult result = enterQueueService.enter(command);

        assertThat(result.token()).isEqualTo("token-1");
        assertThat(result.status()).isEqualTo("WAITING");
        assertThat(result.position()).isEqualTo(3L);
        assertThat(result.expiresAt()).isNull();

        verify(queueQueryPort).findRank("queue-1", "token-1");
    }

    @Test
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

        given(queueCommandPort.enqueueOrGetExisting(any(EnqueueCommand.class)))
                .willReturn(new EnqueueDecision(EnqueueOutcome.ALREADY_ACTIVE, activeEntry));

        EnterQueueResult result = enterQueueService.enter(command);

        assertThat(result.token()).isEqualTo("token-1");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.position()).isEqualTo(0L);
        assertThat(result.expiresAt()).isEqualTo(Instant.parse("2026-04-05T00:11:00Z"));
    }

    @Test
    void enter_whenWaitingButRankMissing_thenReturnNullPosition() {
        EnterQueueCommand command = new EnterQueueCommand("queue-1", 1L);

        QueueEntry entry = QueueEntry.enter(
                "token-1",
                "queue-1",
                1L,
                1L,
                Instant.parse("2026-04-05T00:00:00Z")
        );

        given(queueCommandPort.enqueueOrGetExisting(any(EnqueueCommand.class)))
                .willReturn(new EnqueueDecision(EnqueueOutcome.CREATED, entry));
        given(queueQueryPort.findRank("queue-1", "token-1"))
                .willReturn(null);

        EnterQueueResult result = enterQueueService.enter(command);

        assertThat(result.status()).isEqualTo("WAITING");
        assertThat(result.position()).isNull();
    }
}