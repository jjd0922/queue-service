package com.queue.application.service;

import com.queue.application.dto.*;
import com.queue.application.port.in.ExpireActiveTokensUseCase;
import com.queue.application.port.in.PromoteWaitingQueueUseCase;
import com.queue.application.port.out.QueueExpirationCommandPort;
import com.queue.application.port.out.QueuePromotionCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ExpireActiveTokensService implements ExpireActiveTokensUseCase {

    private final QueueExpirationCommandPort queueExpirationCommandPort;
    private final QueuePromotionCommandPort queuePromotionCommandPort;

    @Override
    public ExpireAndPromoteResult execute(ExpireAndPromoteCommand command) {
        ExpireResult expireResult = queueExpirationCommandPort.expireActiveEntries(
                new ExpireCommand(
                        command.queueId(),
                        command.requestedAt(),
                        command.expireBatchSize()
                )
        );

        int actualExpiredCount = expireResult.actualExpiredCount();
        if (actualExpiredCount <= 0) {
            return ExpireAndPromoteResult.of(
                    command.queueId(),
                    command.expireBatchSize(),
                    0,
                    0,
                    0
            );
        }

        PromoteResult promoteResult = queuePromotionCommandPort.promoteWaitingEntries(
                new PromoteCommand(
                        command.queueId(),
                        command.requestedAt(),
                        command.maxActiveCount(),
                        actualExpiredCount,
                        command.activeTtl()
                )
        );

        return ExpireAndPromoteResult.of(
                command.queueId(),
                command.expireBatchSize(),
                actualExpiredCount,
                promoteResult.requestedCount(),
                promoteResult.promotedCount()
        );
    }
}
