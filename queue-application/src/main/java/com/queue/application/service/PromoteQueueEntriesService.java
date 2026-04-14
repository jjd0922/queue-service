package com.queue.application.service;

import com.queue.application.config.QueuePromotionProperties;
import com.queue.application.dto.PromoteCommand;
import com.queue.application.dto.PromoteResult;
import com.queue.application.port.in.PromoteQueueEntriesUseCase;
import com.queue.application.port.out.QueueCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PromoteQueueEntriesService implements PromoteQueueEntriesUseCase {

    private final QueueCommandPort queueCommandPort;
    private final QueuePromotionProperties properties;

    @Override
    public PromoteResult promote() {
        Instant now = Instant.now();

        PromoteCommand command = new PromoteCommand(
                properties.getQueueId(),
                now,
                properties.getMaxActiveCount(),
                properties.getBatchSize(),
                Duration.ofSeconds(properties.getActiveTtlSeconds())
        );

        return queueCommandPort.promoteWaitingEntries(command);
    }
}