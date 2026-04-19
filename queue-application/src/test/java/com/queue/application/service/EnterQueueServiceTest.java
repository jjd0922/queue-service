package com.queue.application.service;

import com.queue.application.dto.EnterQueueCommand;
import com.queue.application.dto.EnterQueueResult;
import com.queue.application.port.out.QueueEntryCommandPort;
import com.queue.application.port.out.QueueEntryQueryPort;
import com.queue.application.port.out.QueueSequenceGenerator;
import com.queue.application.port.out.QueueTokenGenerator;
import com.queue.application.port.out.WaitingQueuePort;
import com.queue.domain.model.QueueEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnterQueueServiceTest {

    @Mock
    private QueueEntryQueryPort queueEntryQueryPort;

    @Mock
    private QueueEntryCommandPort queueEntryCommandPort;

    @Mock
    private WaitingQueuePort waitingQueuePort;

    @Mock
    private QueueTokenGenerator queueTokenGenerator;

    @Mock
    private QueueSequenceGenerator queueSequenceGenerator;

    private EnterQueueService enterQueueService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-04-04T10:00:00Z"),
                ZoneOffset.UTC
        );

        enterQueueService = new EnterQueueService(
                queueEntryQueryPort,
                queueEntryCommandPort,
                waitingQueuePort,
                queueTokenGenerator,
                queueSequenceGenerator,
                fixedClock
        );
    }

    @Test
    @DisplayName("신규 사용자가 진입하면 entry 를 저장하고 waiting queue 에 enqueue 한다")
    void enter_whenNewUser_thenSaveAndEnqueue() {
        EnterQueueCommand command = new EnterQueueCommand("product:100", 1L);

        when(queueEntryQueryPort.findByQueueIdAndUserId("product:100", 1L))
                .thenReturn(Optional.empty());
        when(queueTokenGenerator.generate()).thenReturn("qt_token_1");
        when(queueSequenceGenerator.nextSequence("product:100")).thenReturn(10L);
        when(waitingQueuePort.findPosition("product:100", "qt_token_1"))
                .thenReturn(Optional.of(1L));

        EnterQueueResult result = enterQueueService.enter(command);

        ArgumentCaptor<QueueEntry> entryCaptor = ArgumentCaptor.forClass(QueueEntry.class);
        verify(queueEntryCommandPort).save(entryCaptor.capture());
        verify(waitingQueuePort).enqueue("product:100", "qt_token_1", 10L);

        QueueEntry saved = entryCaptor.getValue();
        assertThat(saved.getToken()).isEqualTo("qt_token_1");
        assertThat(saved.getQueueId()).isEqualTo("product:100");
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getSequence()).isEqualTo(10L);
        assertThat(saved.isWaiting()).isTrue();

        assertThat(result.token()).isEqualTo("qt_token_1");
        assertThat(result.queueId()).isEqualTo("product:100");
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo("WAITING");
        assertThat(result.position()).isEqualTo(1L);
        assertThat(result.enteredAt()).isEqualTo(Instant.parse("2026-04-04T10:00:00Z"));
        assertThat(result.expiresAt()).isNull();
    }

    @Test
    @DisplayName("기존 WAITING 사용자가 있으면 새로 저장하지 않고 기존 entry 를 반환한다")
    void enter_whenExistingWaiting_thenReturnExisting() {
        QueueEntry existing = QueueEntry.enter(
                "qt_existing",
                "product:100",
                1L,
                5L,
                Instant.parse("2026-04-04T09:55:00Z")
        );

        when(queueEntryQueryPort.findByQueueIdAndUserId("product:100", 1L))
                .thenReturn(Optional.of(existing));
        when(waitingQueuePort.findPosition("product:100", "qt_existing"))
                .thenReturn(Optional.of(3L));

        EnterQueueResult result = enterQueueService.enter(new EnterQueueCommand("product:100", 1L));

        assertThat(result.token()).isEqualTo("qt_existing");
        assertThat(result.status()).isEqualTo("WAITING");
        assertThat(result.position()).isEqualTo(3L);

        verify(queueEntryCommandPort, never()).save(any());
        verify(waitingQueuePort, never()).enqueue(anyString(), anyString(), anyLong());
        verify(queueTokenGenerator, never()).generate();
        verify(queueSequenceGenerator, never()).nextSequence(anyString());
    }

    @Test
    @DisplayName("기존 ACTIVE 사용자가 있으면 position 0 으로 기존 entry 를 반환한다")
    void enter_whenExistingActive_thenReturnExisting() {
        QueueEntry existing = QueueEntry.enter(
                "qt_active",
                "product:100",
                1L,
                5L,
                Instant.parse("2026-04-04T09:55:00Z")
        );
        existing.activate(
                Instant.parse("2026-04-04T09:56:00Z"),
                Instant.parse("2026-04-04T10:10:00Z")
        );

        when(queueEntryQueryPort.findByQueueIdAndUserId("product:100", 1L))
                .thenReturn(Optional.of(existing));

        EnterQueueResult result = enterQueueService.enter(new EnterQueueCommand("product:100", 1L));

        assertThat(result.token()).isEqualTo("qt_active");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.position()).isEqualTo(0L);

        verify(queueEntryCommandPort, never()).save(any());
        verify(waitingQueuePort, never()).enqueue(anyString(), anyString(), anyLong());
        verify(waitingQueuePort, never()).findPosition(anyString(), anyString());
        verify(queueTokenGenerator, never()).generate();
        verify(queueSequenceGenerator, never()).nextSequence(anyString());
    }
}