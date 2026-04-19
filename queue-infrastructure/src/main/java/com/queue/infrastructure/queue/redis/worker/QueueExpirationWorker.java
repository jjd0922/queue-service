package com.queue.infrastructure.queue.redis.worker;

import com.queue.application.dto.ExpireAndPromoteCommand;
import com.queue.application.dto.ExpireAndPromoteResult;
import com.queue.application.port.in.ExpireAndPromoteUseCase;
import com.queue.infrastructure.config.QueueExpirationWorkerProperties;
import com.queue.infrastructure.config.QueuePromotionWorkerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Component
public class QueueExpirationWorker {

    private final ExpireAndPromoteUseCase expireAndPromoteUseCase;
    private final QueueExpirationWorkerProperties expirationProperties;
    private final QueuePromotionWorkerProperties promotionProperties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${queue.worker.expiration.fixed-delay-ms:1000}")
    public void execute() {
        if (!expirationProperties.isEnabled()) {
            return;
        }

        ExpireAndPromoteResult result = expireAndPromoteUseCase.execute(
                new ExpireAndPromoteCommand(
                        promotionProperties.getQueueId(),
                        Instant.now(clock),
                        expirationProperties.getBatchSize(),
                        promotionProperties.getBatchSize(),
                        promotionProperties.getMaxActiveCount(),
                        promotionProperties.activeTtl()
                )
        );

        if (result.actualExpiredCount() > 0 || result.actualPromotedCount() > 0) {
            log.info(
                    "queue expiration worker executed. queueId={}, expiredCount={}, promotedCount={}",
                    result.queueId(),
                    result.actualExpiredCount(),
                    result.actualPromotedCount()
            );
        }
    }
}
