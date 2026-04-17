package com.queue.application.service;

import com.queue.application.config.QueuePromotionProperties;
import com.queue.application.dto.PromoteCommand;
import com.queue.application.dto.PromoteResult;
import com.queue.application.port.in.PromoteQueueEntriesUseCase;
import com.queue.application.port.out.QueuePromotionCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PromoteQueueEntriesService implements PromoteQueueEntriesUseCase {

    private final QueuePromotionCommandPort queuePromotionCommandPort;
    private final QueuePromotionProperties properties;

    public PromoteResult promote() {
        return queuePromotionCommandPort.promoteWaitingEntries(
                new PromoteCommand(
                        properties.getQueueId(),
                        Instant.now(),
                        properties.getMaxActiveCount(),
                        properties.getBatchSize(),
                        properties.activeTtl()
                )
        );
    }
}