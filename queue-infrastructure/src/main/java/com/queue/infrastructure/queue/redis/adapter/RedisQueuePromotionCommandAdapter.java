package com.queue.infrastructure.queue.redis.adapter;

import com.queue.application.dto.PromoteCommand;
import com.queue.application.dto.PromoteResult;
import com.queue.application.port.out.QueuePromotionCommandPort;
import com.queue.domain.model.QueueEntryStatus;
import com.queue.infrastructure.queue.redis.generator.RedisQueueKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Component
public class RedisQueuePromotionCommandAdapter implements QueuePromotionCommandPort {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> promoteWaitingEntriesScript;
    private final RedisQueueKeyGenerator keyGenerator;

    @Override
    public PromoteResult promoteWaitingEntries(PromoteCommand request) {
        Instant now = request.requestedAt();
        Instant expiresAt = now.plus(request.activeTtl());

        List<String> keys = List.of(
                keyGenerator.waitingQueueKey(request.queueId()),
                keyGenerator.activeQueueKey(request.queueId()),
                keyGenerator.activeExpiryKey(request.queueId())
        );

        List<String> args = List.of(
                keyGenerator.entryKeyPrefix(),
                QueueEntryStatus.ACTIVE.name(),
                now.toString(),
                expiresAt.toString(),
                String.valueOf(expiresAt.toEpochMilli()),
                String.valueOf(request.maxActiveCount()),
                String.valueOf(request.promoteBatchSize())
        );

        Long promotedCount = stringRedisTemplate.execute(
                promoteWaitingEntriesScript,
                keys,
                args.toArray()
        );

        return new PromoteResult(
                request.queueId(),
                request.promoteBatchSize(),
                promotedCount == null ? 0 : promotedCount.intValue()
        );
    }
}