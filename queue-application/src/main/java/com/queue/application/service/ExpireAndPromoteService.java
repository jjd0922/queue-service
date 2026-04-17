package com.queue.application.service;

import com.queue.application.dto.*;
import com.queue.application.port.in.ExpireAndPromoteUseCase;
import com.queue.application.port.out.QueueExpirationCommandPort;
import com.queue.application.port.out.QueuePromotionCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ExpireAndPromoteService implements ExpireAndPromoteUseCase {

    private final QueuePromotionCommandPort queuePromotionCommandPort;
    private final QueueExpirationCommandPort queueExpirationCommandPort;

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
        PromoteResult promoteResult = queuePromotionCommandPort.promoteWaitingEntries(
                new PromoteCommand(
                        command.queueId(),
                        command.requestedAt(),
                        command.maxActiveCount(),
                        command.promoteBatchSize(),
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
