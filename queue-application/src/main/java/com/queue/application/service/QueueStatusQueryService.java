package com.queue.application.service;

import com.queue.application.dto.GetQueueStatusQuery;
import com.queue.application.port.in.GetQueueStatusUseCase;
import com.queue.application.port.out.QueueStatusQueryPort;
import com.queue.domain.exception.QueueErrorCode;
import com.queue.domain.exception.QueueException;
import com.queue.domain.model.QueueEntrySnapshot;
import com.queue.domain.model.QueueEntryStatus;
import com.queue.application.dto.QueueStatusResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueueStatusQueryService implements GetQueueStatusUseCase {

    private final QueueStatusQueryPort queueStatusQueryPort;

    @Override
    public QueueStatusResult getQueueStatus(GetQueueStatusQuery query) {
        QueueEntrySnapshot entry = queueStatusQueryPort.findEntry(query.queueToken())
                .orElseThrow(() -> new QueueException(QueueErrorCode.QUEUE_ENTRY_NOT_FOUND));

        boolean active = queueStatusQueryPort.isActive(query.queueName(), query.queueToken());
        if (active) {
            return new QueueStatusResult(
                    query.queueName(),
                    query.queueToken(),
                    QueueEntryStatus.ACTIVE.name(),
                    null,
                    0L,
                    entry.enteredAt(),
                    entry.activatedAt(),
                    entry.expiresAt()
            );
        }

        return queueStatusQueryPort.findWaitingPosition(query.queueName(), query.queueToken())
                .map(position -> new QueueStatusResult(
                        query.queueName(),
                        query.queueToken(),
                        QueueEntryStatus.WAITING.name(),
                        position,
                        position - 1,
                        entry.enteredAt(),
                        entry.activatedAt(),
                        entry.expiresAt()
                ))
                .orElseThrow(() -> new QueueException(QueueErrorCode.QUEUE_ENTRY_NOT_FOUND));
    }
}
