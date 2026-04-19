package com.queue.application.service;

import com.queue.application.dto.ExpireAndPromoteCommand;
import com.queue.application.dto.ExpireAndPromoteResult;
import com.queue.application.dto.ExpireCommand;
import com.queue.application.dto.ExpireResult;
import com.queue.application.dto.PromoteCommand;
import com.queue.application.dto.PromoteResult;
import com.queue.application.port.out.QueueExpirationCommandPort;
import com.queue.application.port.out.QueuePromotionCommandPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExpireActiveTokensServiceTest {

    private QueueExpirationCommandPort queueExpirationCommandPort;
    private QueuePromotionCommandPort queuePromotionCommandPort;
    private ExpireActiveTokensService service;

    @BeforeEach
    void setUp() {
        queueExpirationCommandPort = mock(QueueExpirationCommandPort.class);
        queuePromotionCommandPort = mock(QueuePromotionCommandPort.class);

        service = new ExpireActiveTokensService(
                queueExpirationCommandPort,
                queuePromotionCommandPort
        );
    }

    @Test
    @DisplayName("만료된 엔트리가 없으면 재승격을 호출하지 않는다")
    void execute_whenNoExpiredEntries_thenSkipPromotion() {
        Instant now = Instant.parse("2026-04-16T10:00:00Z");

        when(queueExpirationCommandPort.expireActiveEntries(any(ExpireCommand.class)))
                .thenReturn(new ExpireResult("default", 50, 0));

        ExpireAndPromoteResult result = service.execute(
                new ExpireAndPromoteCommand(
                        "default",
                        now,
                        50,
                        100,
                        Duration.ofSeconds(180)
                )
        );

        verify(queueExpirationCommandPort).expireActiveEntries(any(ExpireCommand.class));
        verify(queuePromotionCommandPort, never()).promoteWaitingEntries(any(PromoteCommand.class));

        assertThat(result.queueId()).isEqualTo("default");
        assertThat(result.requestedExpireBatchSize()).isEqualTo(50);
        assertThat(result.actualExpiredCount()).isZero();
        assertThat(result.requestedPromoteCount()).isZero();
        assertThat(result.actualPromotedCount()).isZero();
    }

    @Test
    @DisplayName("만료된 수만큼 PromoteCommand 를 생성해 promotion port 로 전달한다")
    void execute_buildsPromoteCommandAndDelegates() {
        Instant now = Instant.parse("2026-04-16T10:00:00Z");

        when(queueExpirationCommandPort.expireActiveEntries(any(ExpireCommand.class)))
                .thenReturn(new ExpireResult("default", 50, 3));

        when(queuePromotionCommandPort.promoteWaitingEntries(any(PromoteCommand.class)))
                .thenReturn(new PromoteResult("default", 3, 2));

        ExpireAndPromoteResult result = service.execute(
                new ExpireAndPromoteCommand(
                        "default",
                        now,
                        50,
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
        assertThat(actual.promoteBatchSize()).isEqualTo(3);
        assertThat(actual.activeTtl()).isEqualTo(Duration.ofSeconds(180));

        assertThat(result.queueId()).isEqualTo("default");
        assertThat(result.requestedExpireBatchSize()).isEqualTo(50);
        assertThat(result.actualExpiredCount()).isEqualTo(3);
        assertThat(result.requestedPromoteCount()).isEqualTo(3);
        assertThat(result.actualPromotedCount()).isEqualTo(2);
    }
}