package com.queue.application.service;

import com.queue.application.dto.*;
import com.queue.application.port.in.EnterQueueUseCase;
import com.queue.application.port.out.*;

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
    private final Clock clock;

    @Override
    public EnterQueueResult enter(EnterQueueCommand command) {
        EnqueueDecision decision = queueEnqueueCommandPort.enqueueOrGetExisting(
                new EnqueueCommand(
                        command.queueId(),
                        command.userId(),
                        Instant.now(clock)
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

        return EnterQueueResult.of(decision, position);
    }
}