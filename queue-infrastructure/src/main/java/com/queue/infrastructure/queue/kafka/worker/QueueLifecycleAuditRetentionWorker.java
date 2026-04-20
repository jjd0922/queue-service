package com.queue.infrastructure.queue.kafka.worker;

import com.queue.application.dto.command.PurgeQueueLifecycleAuditCommand;
import com.queue.application.port.in.PurgeQueueLifecycleAuditUseCase;
import com.queue.infrastructure.config.QueueAuditRetentionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueLifecycleAuditRetentionWorker {

    private final PurgeQueueLifecycleAuditUseCase purgeQueueLifecycleAuditUseCase;
    private final QueueAuditRetentionProperties queueAuditRetentionProperties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${queue.audit.retention.fixed-delay-ms:300000}")
    public void execute() {
        if (!queueAuditRetentionProperties.isEnabled()) {
            return;
        }

        int purged = purgeQueueLifecycleAuditUseCase.purge(
                new PurgeQueueLifecycleAuditCommand(
                        queueAuditRetentionProperties.getRetentionDays(),
                        queueAuditRetentionProperties.getBatchSize(),
                        Instant.now(clock)
                )
        );

        if (purged > 0) {
            log.info(
                    "queue lifecycle audit retention executed. retentionDays={}, purgedCount={}",
                    queueAuditRetentionProperties.getRetentionDays(),
                    purged
            );
        }
    }
}
