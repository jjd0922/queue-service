package com.queue.application.service;

import com.queue.application.dto.*;
import com.queue.application.port.in.ExpireAndPromoteUseCase;
import com.queue.application.port.out.QueueCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ExpireAndPromoteService implements ExpireAndPromoteUseCase {

    private final QueueCommandPort queueCommandPort;

    @Override
    public ExpireAndPromoteResult execute(ExpireAndPromoteCommand command) {
        ExpireResult expireResult = queueCommandPort.expireActiveEntries(
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

        PromoteResult promoteResult = queueCommandPort.promoteWaitingEntries(
                new PromoteCommand(
                        command.queueId(),
                        command.requestedAt(),
                        actualExpiredCount,
                        command.maxActiveCount(),
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
