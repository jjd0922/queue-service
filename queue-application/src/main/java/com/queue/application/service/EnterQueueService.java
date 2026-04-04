package com.queue.application.service;

import com.queue.application.dto.EnterQueueCommand;
import com.queue.application.dto.EnterQueueResult;
import com.queue.application.port.in.EnterQueueUseCase;
import com.queue.application.port.out.*;

import com.queue.domain.model.QueueEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EnterQueueService implements EnterQueueUseCase {

    private final QueueEntryQueryPort queueEntryQueryPort;
    private final QueueEntryCommandPort queueEntryCommandPort;
    private final WaitingQueuePort waitingQueuePort;
    private final QueueTokenGenerator queueTokenGenerator;
    private final QueueSequenceGenerator queueSequenceGenerator;
    private final Clock clock;

    @Override
    public EnterQueueResult enter(EnterQueueCommand command) {
        QueueEntry existing = queueEntryQueryPort
                .findByQueueIdAndUserId(command.queueId(), command.userId())
                .orElse(null);

        if (existing != null) {
            Long position = resolvePosition(existing);
            return EnterQueueResult.of(existing, position);
        }

        Instant now = Instant.now(clock);
        String token = queueTokenGenerator.generate();
        Long sequence = queueSequenceGenerator.nextSequence(command.queueId());

        QueueEntry entry = QueueEntry.enter(
                token,
                command.queueId(),
                command.userId(),
                sequence,
                now
        );

        queueEntryCommandPort.save(entry);
        waitingQueuePort.enqueue(entry.getQueueId(), entry.getToken(), entry.getSequence());

        Long position = waitingQueuePort.findPosition(entry.getQueueId(), entry.getToken()).orElse(null);
        return EnterQueueResult.of(entry, position);
    }

    private Long resolvePosition(QueueEntry entry) {
        if (entry.isWaiting()) {
            return waitingQueuePort.findPosition(entry.getQueueId(), entry.getToken()).orElse(null);
        }
        if (entry.isActive()) {
            return 0L;
        }
        return null;
    }
}