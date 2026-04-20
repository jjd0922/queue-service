package com.queue.application.service;

import com.queue.application.dto.command.ExpireAndPromoteCommand;
import com.queue.application.dto.result.ExpireAndPromoteResult;
import com.queue.application.dto.command.ExpireCommand;
import com.queue.application.dto.result.ExpireResult;
import com.queue.application.dto.command.PromoteCommand;
import com.queue.application.dto.result.PromoteResult;
import com.queue.application.port.out.QueueExpirationCommandPort;
import com.queue.application.port.out.QueueLifecycleEventPort;
import com.queue.application.port.out.QueuePromotionCommandPort;
import com.queue.domain.event.QueueLifecycleEvent;
import com.queue.domain.model.QueueEntry;
import com.queue.domain.model.QueueEntryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpireAndPromoteServiceTest {

    private QueuePromotionCommandPort queuePromotionCommandPort;
    private QueueExpirationCommandPort queueExpirationCommandPort;
    private QueueLifecycleEventPort queueLifecycleEventPort;
    private ExpireAndPromoteService service;

    @BeforeEach
    void setUp() {
        queuePromotionCommandPort = mock(QueuePromotionCommandPort.class);
        queueExpirationCommandPort = mock(QueueExpirationCommandPort.class);
        queueLifecycleEventPort = mock(QueueLifecycleEventPort.class);

        service = new ExpireAndPromoteService(
                queuePromotionCommandPort,
                queueExpirationCommandPort,
                queueLifecycleEventPort
        );
    }

    @Test
    @DisplayName("만료 건수가 없어도 승격을 시도한다")
    void execute_whenNoExpiredEntries_thenPromotesByBatchSize() {
        Instant now = Instant.parse("2026-04-16T10:00:00Z");

        when(queueExpirationCommandPort.expireActiveEntries(any(ExpireCommand.class)))
                .thenReturn(new ExpireResult("default", 50, 0));
        when(queuePromotionCommandPort.promoteWaitingEntries(any(PromoteCommand.class)))
                .thenReturn(new PromoteResult("default", 10, promotedEntries(now)));

        ExpireAndPromoteResult result = service.execute(
                new ExpireAndPromoteCommand(
                        "default",
                        now,
                        50,
                        10,
                        100,
                        Duration.ofSeconds(180)
                )
        );

        verify(queueExpirationCommandPort).expireActiveEntries(any(ExpireCommand.class));
        verify(queuePromotionCommandPort).promoteWaitingEntries(any(PromoteCommand.class));

        assertThat(result.queueId()).isEqualTo("default");
        assertThat(result.requestedExpireBatchSize()).isEqualTo(50);
        assertThat(result.actualExpiredCount()).isZero();
        assertThat(result.requestedPromoteCount()).isEqualTo(10);
        assertThat(result.actualPromotedCount()).isEqualTo(2);
        verify(queueLifecycleEventPort, times(2)).publish(any(QueueLifecycleEvent.class));
    }

    @Test
    @DisplayName("설정된 promote batch 크기로 승격을 호출한다")
    void execute_buildsPromoteCommandAndDelegates() {
        Instant now = Instant.parse("2026-04-16T10:00:00Z");

        when(queueExpirationCommandPort.expireActiveEntries(any(ExpireCommand.class)))
                .thenReturn(new ExpireResult("default", 50, 3));
        when(queuePromotionCommandPort.promoteWaitingEntries(any(PromoteCommand.class)))
                .thenReturn(new PromoteResult("default", 10, promotedEntries(now)));

        ExpireAndPromoteResult result = service.execute(
                new ExpireAndPromoteCommand(
                        "default",
                        now,
                        50,
                        10,
                        100,
                        Duration.ofSeconds(180)
                )
        );

        ArgumentCaptor<PromoteCommand> captor = ArgumentCaptor.forClass(PromoteCommand.class);
        verify(queuePromotionCommandPort).promoteWaitingEntries(captor.capture());

        PromoteCommand actual = captor.getValue();

        assertThat(actual.queueId()).isEqualTo("default");
        assertThat(actual.requestedAt()).isEqualTo(now);
        assertThat(actual.maxActiveCount()).isEqualTo(100);
        assertThat(actual.promoteBatchSize()).isEqualTo(10);
        assertThat(actual.activeTtl()).isEqualTo(Duration.ofSeconds(180));

        assertThat(result.queueId()).isEqualTo("default");
        assertThat(result.requestedExpireBatchSize()).isEqualTo(50);
        assertThat(result.actualExpiredCount()).isEqualTo(3);
        assertThat(result.requestedPromoteCount()).isEqualTo(10);
        assertThat(result.actualPromotedCount()).isEqualTo(2);
        verify(queueLifecycleEventPort, times(2)).publish(any(QueueLifecycleEvent.class));
    }

    private List<QueueEntry> promotedEntries(Instant now) {
        return List.of(
                QueueEntry.restore(
                        "token-1",
                        "default",
                        1L,
                        QueueEntryStatus.ACTIVE,
                        1L,
                        now.minusSeconds(60),
                        now,
                        now.plusSeconds(180),
                        now
                ),
                QueueEntry.restore(
                        "token-2",
                        "default",
                        2L,
                        QueueEntryStatus.ACTIVE,
                        2L,
                        now.minusSeconds(55),
                        now,
                        now.plusSeconds(180),
                        now
                )
        );
    }
}
