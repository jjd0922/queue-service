package com.queue.application.service;

import com.queue.application.dto.GetQueueStatusQuery;
import com.queue.application.dto.QueueStatusResult;
import com.queue.application.port.out.QueueStatusQueryPort;
import com.queue.domain.exception.QueueException;
import com.queue.domain.model.QueueEntrySnapshot;
import com.queue.domain.model.QueueEntryStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class QueueStatusQueryServiceTest {

    @Mock
    private QueueStatusQueryPort queueStatusQueryPort;

    @InjectMocks
    private QueueStatusQueryService queueStatusQueryService;

    @Nested
    class GetQueueStatus {

        @Test
        @DisplayName("대기 중인 토큰이면 순번과 앞 대기 인원을 반환한다")
        void returnsWaitingStatusWithPosition() {
            // given
            String queueName = "concert-queue";
            String token = "token-1";
            Instant enteredAt = Instant.parse("2026-04-06T10:00:00Z");

            QueueEntrySnapshot snapshot = new QueueEntrySnapshot(
                    token,
                    QueueEntryStatus.WAITING,
                    enteredAt,
                    null,
                    null
            );

            given(queueStatusQueryPort.findEntry(token)).willReturn(Optional.of(snapshot));
            given(queueStatusQueryPort.isActive(queueName, token)).willReturn(false);
            given(queueStatusQueryPort.findWaitingPosition(queueName, token)).willReturn(Optional.of(5L));

            // when
            QueueStatusResult result = queueStatusQueryService.getQueueStatus(
                    new GetQueueStatusQuery(queueName, token)
            );

            // then
            assertThat(result.queueName()).isEqualTo(queueName);
            assertThat(result.queueToken()).isEqualTo(token);
            assertThat(result.status()).isEqualTo(QueueEntryStatus.WAITING.name());
            assertThat(result.position()).isEqualTo(5L);
            assertThat(result.aheadCount()).isEqualTo(4L);
            assertThat(result.enteredAt()).isEqualTo(enteredAt);
            assertThat(result.activatedAt()).isNull();
            assertThat(result.expiresAt()).isNull();
        }

        @Test
        @DisplayName("활성 상태 토큰이면 입장 가능 상태와 만료 정보를 반환한다")
        void returnsActiveStatus() {
            // given
            String queueName = "concert-queue";
            String token = "token-2";
            Instant enteredAt = Instant.parse("2026-04-06T10:00:00Z");
            Instant activatedAt = Instant.parse("2026-04-06T10:05:00Z");
            Instant expiresAt = Instant.parse("2026-04-06T10:15:00Z");

            QueueEntrySnapshot snapshot = new QueueEntrySnapshot(
                    token,
                    QueueEntryStatus.ACTIVE,
                    enteredAt,
                    activatedAt,
                    expiresAt
            );

            given(queueStatusQueryPort.findEntry(token)).willReturn(Optional.of(snapshot));
            given(queueStatusQueryPort.isActive(queueName, token)).willReturn(true);

            // when
            QueueStatusResult result = queueStatusQueryService.getQueueStatus(
                    new GetQueueStatusQuery(queueName, token)
            );

            // then
            assertThat(result.queueName()).isEqualTo(queueName);
            assertThat(result.queueToken()).isEqualTo(token);
            assertThat(result.status()).isEqualTo(QueueEntryStatus.ACTIVE.name());
            assertThat(result.position()).isNull();
            assertThat(result.aheadCount()).isEqualTo(0L);
            assertThat(result.enteredAt()).isEqualTo(enteredAt);
            assertThat(result.activatedAt()).isEqualTo(activatedAt);
            assertThat(result.expiresAt()).isEqualTo(expiresAt);
        }

        @Test
        @DisplayName("엔트리가 없으면 예외를 던진다")
        void throwsExceptionWhenEntryNotFound() {
            // given
            String queueName = "concert-queue";
            String token = "missing-token";

            given(queueStatusQueryPort.findEntry(token)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> queueStatusQueryService.getQueueStatus(
                    new GetQueueStatusQuery(queueName, token)
            )).isInstanceOf(QueueException.class);
        }

        @Test
        @DisplayName("활성 상태도 아니고 대기열 순번도 없으면 예외를 던진다")
        void throwsExceptionWhenNeitherActiveNorWaiting() {
            // given
            String queueName = "concert-queue";
            String token = "token-3";
            Instant enteredAt = Instant.parse("2026-04-06T10:00:00Z");

            QueueEntrySnapshot snapshot = new QueueEntrySnapshot(
                    token,
                    QueueEntryStatus.WAITING,
                    enteredAt,
                    null,
                    null
            );

            given(queueStatusQueryPort.findEntry(token)).willReturn(Optional.of(snapshot));
            given(queueStatusQueryPort.isActive(queueName, token)).willReturn(false);
            given(queueStatusQueryPort.findWaitingPosition(queueName, token)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> queueStatusQueryService.getQueueStatus(
                    new GetQueueStatusQuery(queueName, token)
            )).isInstanceOf(QueueException.class);
        }
    }
}
