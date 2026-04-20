package com.queue.infrastructure.queue.redis.adapter;

import com.queue.application.dto.ExpireCommand;
import com.queue.application.dto.ExpireResult;
import com.queue.application.port.out.QueueExpirationCommandPort;
import com.queue.domain.model.QueueEntryStatus;
import com.queue.infrastructure.queue.redis.generator.RedisQueueKeyGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class RedisQueueExpirationCommandAdapter implements QueueExpirationCommandPort {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> expireActiveEntriesScript;
    private final RedisQueueKeyGenerator keyGenerator;

    public RedisQueueExpirationCommandAdapter(
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("expireActiveEntriesScript") RedisScript<Long> expireActiveEntriesScript,
            RedisQueueKeyGenerator keyGenerator
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.expireActiveEntriesScript = expireActiveEntriesScript;
        this.keyGenerator = keyGenerator;
    }

    @Override
    public ExpireResult expireActiveEntries(ExpireCommand request) {
        Instant now = request.requestedAt();

        List<String> keys = List.of(
                keyGenerator.activeExpiryKey(request.queueId()),
                keyGenerator.activeQueueKey(request.queueId())
        );

        List<String> args = List.of(
                keyGenerator.entryKeyPrefix(),
                QueueEntryStatus.EXPIRED.name(),
                now.toString(),
                String.valueOf(now.toEpochMilli()),
                String.valueOf(request.expireBatchSize()),
                keyGenerator.userIndexKeyPrefix(request.queueId())
        );

        Long expiredCount = stringRedisTemplate.execute(
                expireActiveEntriesScript,
                keys,
                args.toArray()
        );

        return new ExpireResult(
                request.queueId(),
                request.expireBatchSize(),
                expiredCount == null ? 0 : expiredCount.intValue()
        );
    }
}
