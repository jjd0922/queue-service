package com.queue.application.service;

import com.queue.application.dto.ExpireActiveTokensCommand;
import com.queue.application.dto.ExpireActiveTokensResult;
import com.queue.application.port.in.ExpireActiveTokensUseCase;
import com.queue.application.port.in.PromoteWaitingQueueUseCase;
import com.queue.application.port.out.QueueExpirationCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ExpireActiveTokensService implements ExpireActiveTokensUseCase {

    private final QueueExpirationCommandPort queueExpirationCommandPort;
    private final PromoteWaitingQueueUseCase promoteWaitingQueueUseCase;
    private final Clock clock;

    @Override
    public ExpireActiveTokensResult expire(ExpireActiveTokensCommand command) {
        Instant now = Instant.now(clock);

        long expiredCount = queueExpirationCommandPort.expireActiveTokens(now, command.batchSize());

        int promotedCount = 0;
        if (expiredCount > 0) {
            int requestedCount = expiredCount > Integer.MAX_VALUE
                    ? Integer.MAX_VALUE
                    : (int) expiredCount;
            promotedCount = promoteWaitingQueueUseCase.promote(requestedCount);
        }

        return ExpireActiveTokensResult.of(expiredCount, promotedCount);
    }
}
