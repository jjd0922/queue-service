package com.queue.infrastructure.queue.redis.worker;

import com.queue.application.dto.command.ExpireAndPromoteCommand;
import com.queue.application.dto.result.ExpireAndPromoteResult;
import com.queue.application.port.in.ExpireAndPromoteUseCase;
import com.queue.infrastructure.config.QueueExpirationWorkerProperties;
import com.queue.infrastructure.config.QueuePromotionWorkerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QueueExpirationWorkerTest {

    private ExpireAndPromoteUseCase expireAndPromoteUseCase;
    private QueueExpirationWorkerProperties expirationProperties;
    private QueuePromotionWorkerProperties promotionProperties;
    private Clock clock;
    private QueueExpirationWorker worker;

    @BeforeEach
    void setUp() {
        expireAndPromoteUseCase = mock(ExpireAndPromoteUseCase.class);

        expirationProperties = new QueueExpirationWorkerProperties();
        expirationProperties.setEnabled(true);
        expirationProperties.setBatchSize(50);
        expirationProperties.setFixedDelayMs(1000L);

        promotionProperties = new QueuePromotionWorkerProperties();
        promotionProperties.setQueueId("default");
        promotionProperties.setBatchSize(50);
        promotionProperties.setMaxActiveCount(100);
        promotionProperties.setActiveTtlSeconds(180L);

        clock = Clock.fixed(Instant.parse("2026-04-16T10:00:00Z"), ZoneOffset.UTC);

        worker = new QueueExpirationWorker(
                expireAndPromoteUseCase,
                expirationProperties,
                promotionProperties,
                clock
        );
    }

    @Test
    @DisplayName("설정값으로 ExpireAndPromoteCommand 를 생성해 use case 로 전달한다")
    void execute_buildsCommandAndDelegates() {
        when(expireAndPromoteUseCase.execute(any(ExpireAndPromoteCommand.class)))
                .thenReturn(new ExpireAndPromoteResult("default", 50, 2, 2, 2));

        worker.execute();

        ArgumentCaptor<ExpireAndPromoteCommand> captor =
                ArgumentCaptor.forClass(ExpireAndPromoteCommand.class);
        verify(expireAndPromoteUseCase).execute(captor.capture());

        ExpireAndPromoteCommand actual = captor.getValue();

        assertThat(actual.queueId()).isEqualTo("default");
        assertThat(actual.requestedAt()).isEqualTo(Instant.parse("2026-04-16T10:00:00Z"));
        assertThat(actual.expireBatchSize()).isEqualTo(50);
        assertThat(actual.promoteBatchSize()).isEqualTo(50);
        assertThat(actual.maxActiveCount()).isEqualTo(100);
        assertThat(actual.activeTtl().getSeconds()).isEqualTo(180);
    }

    @Test
    @DisplayName("비활성화 상태이면 use case 를 호출하지 않는다")
    void execute_doesNothing_whenWorkerDisabled() {
        expirationProperties.setEnabled(false);

        worker.execute();

        verifyNoInteractions(expireAndPromoteUseCase);
    }
}
