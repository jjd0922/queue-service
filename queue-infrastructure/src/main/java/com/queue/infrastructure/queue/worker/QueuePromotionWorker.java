package com.queue.infrastructure.queue.worker;

import com.queue.application.dto.PromoteResult;
import com.queue.application.port.in.PromoteQueueEntriesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "queue.worker.promotion.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class QueuePromotionWorker {

    private final PromoteQueueEntriesUseCase promoteQueueEntriesUseCase;

    @Scheduled(fixedDelayString = "${queue.worker.promotion.fixed-delay-ms:1000}")
    public void promote() {
        PromoteResult result = promoteQueueEntriesUseCase.promote();

        if (result.hasPromoted()) {
            log.info(
                    "queue promotion completed queueId={}, requestedCount={}, promotedCount={}",
                    result.queueId(),
                    result.requestedCount(),
                    result.promotedCount()
            );
        }
    }
}