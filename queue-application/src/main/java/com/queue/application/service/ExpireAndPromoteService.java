package com.queue.application.service;

import com.queue.application.dto.command.ExpireAndPromoteCommand;
import com.queue.application.dto.command.ExpireCommand;
import com.queue.application.dto.command.PromoteCommand;
import com.queue.application.dto.result.ExpireAndPromoteResult;
import com.queue.application.dto.result.ExpireResult;
import com.queue.application.dto.result.PromoteResult;
import com.queue.application.port.in.ExpireAndPromoteUseCase;
import com.queue.application.port.out.QueueExpirationCommandPort;
import com.queue.application.port.out.QueueLifecycleEventPort;
import com.queue.application.port.out.QueuePromotionCommandPort;
import com.queue.domain.event.QueueLifecycleEvent;
import com.queue.domain.model.QueueEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@RequiredArgsConstructor
@Service
public class ExpireAndPromoteService implements ExpireAndPromoteUseCase {

    private final QueuePromotionCommandPort queuePromotionCommandPort;
    private final QueueExpirationCommandPort queueExpirationCommandPort;
    private final QueueLifecycleEventPort queueLifecycleEventPort;

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

        publishAdmittedEvents(promoteResult.promotedEntries(), command.requestedAt());

        return ExpireAndPromoteResult.of(
                command.queueId(),
                command.expireBatchSize(),
                actualExpiredCount,
                promoteResult.requestedCount(),
                promoteResult.promotedCount()
        );
    }

    private void publishAdmittedEvents(Iterable<QueueEntry> promotedEntries, Instant occurredAt) {
        for (QueueEntry entry : promotedEntries) {
            queueLifecycleEventPort.publish(
                    QueueLifecycleEvent.admitted(
                            entry.getToken(),
                            entry.getUserId(),
                            entry.getStatus(),
                            entry.getSequence(),
                            occurredAt
                    )
            );
        }
    }
}
