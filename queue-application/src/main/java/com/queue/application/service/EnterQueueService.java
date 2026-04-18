package com.queue.application.service;

import com.queue.application.dto.*;
import com.queue.application.port.in.EnterQueueUseCase;
import com.queue.application.port.out.*;

import com.queue.domain.event.QueueLifecycleEvent;
import com.queue.domain.model.EnqueueDecision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EnterQueueService implements EnterQueueUseCase {

    private final QueueEnqueueCommandPort queueEnqueueCommandPort;
    private final QueueQueryPort queueQueryPort;
    private final QueueLifecycleEventPort queueLifecycleEventPort;
    private final Clock clock;

    @Override
    public EnterQueueResult enter(EnterQueueCommand command) {
        Instant now = Instant.now(clock);
        EnqueueDecision decision = queueEnqueueCommandPort.enqueueOrGetExisting(
                new EnqueueCommand(
                        command.queueId(),
                        command.userId(),
                        now
                )
        );

        Long position = null;
        if (decision.entry().isWaiting()) {
            position = queueQueryPort.findRank(
                    decision.entry().getQueueId(),
                    decision.entry().getToken()
            );
        } else if (decision.entry().isActive()) {
            position = 0L;
        }

        if (decision.isNewlyCreated()) {
            queueLifecycleEventPort.publish(
                    QueueLifecycleEvent.entered(
                            decision.entry().getToken(),
                            decision.entry().getUserId(),
                            decision.entry().getStatus(),
                            decision.entry().getSequence(),
                            now
                    )
            );
        }

        return EnterQueueResult.of(decision, position);
    }
}
