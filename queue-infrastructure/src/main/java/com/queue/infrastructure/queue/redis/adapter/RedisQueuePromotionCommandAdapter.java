package com.queue.infrastructure.queue.redis.adapter;

import com.queue.application.dto.command.PromoteCommand;
import com.queue.application.dto.result.PromoteResult;
import com.queue.application.port.out.QueuePromotionCommandPort;
import com.queue.domain.model.QueueEntry;
import com.queue.domain.model.QueueEntryStatus;
import com.queue.infrastructure.queue.redis.generator.RedisQueueKeyGenerator;
import com.queue.infrastructure.queue.redis.mapper.RedisQueueEntryMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class RedisQueuePromotionCommandAdapter implements QueuePromotionCommandPort {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<List> promoteWaitingEntriesScript;
    private final RedisQueueKeyGenerator keyGenerator;
    private final RedisQueueEntryMapper redisQueueEntryMapper;

    public RedisQueuePromotionCommandAdapter(
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("promoteWaitingEntriesScript") RedisScript<List> promoteWaitingEntriesScript,
            RedisQueueKeyGenerator keyGenerator,
            RedisQueueEntryMapper redisQueueEntryMapper
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.promoteWaitingEntriesScript = promoteWaitingEntriesScript;
        this.keyGenerator = keyGenerator;
        this.redisQueueEntryMapper = redisQueueEntryMapper;
    }


    @Override
    public PromoteResult promoteWaitingEntries(PromoteCommand command) {
        Instant now = command.requestedAt();
        Instant expiresAt = now.plus(command.activeTtl());

        List<String> keys = List.of(
                keyGenerator.waitingQueueKey(command.queueId()),
                keyGenerator.activeQueueKey(command.queueId()),
                keyGenerator.activeExpiryKey(command.queueId())
        );

        List<String> args = List.of(
                keyGenerator.entryKeyPrefix(),
                QueueEntryStatus.ACTIVE.name(),
                now.toString(),
                expiresAt.toString(),
                String.valueOf(expiresAt.toEpochMilli()),
                String.valueOf(command.maxActiveCount()),
                String.valueOf(command.promoteBatchSize())
        );

        Object promotedTokensRaw = stringRedisTemplate.execute(
                promoteWaitingEntriesScript,
                keys,
                args.toArray()
        );

        List<String> promotedTokens = toTokenList(promotedTokensRaw);

        List<QueueEntry> promotedEntries = promotedTokens.isEmpty()
                ? List.of()
                : promotedTokens.stream()
                .map(keyGenerator::entryKey)
                .map(entryKey -> stringRedisTemplate.opsForHash().entries(entryKey))
                .filter(hash -> !hash.isEmpty())
                .map(hash -> redisQueueEntryMapper.from(command.queueId(), hash))
                .toList();

        return new PromoteResult(
                command.queueId(),
                command.promoteBatchSize(),
                promotedEntries
        );
    }

    private List<String> toTokenList(Object rawResult) {
        if (!(rawResult instanceof List<?> rawList)) {
            return List.of();
        }

        return rawList.stream()
                .map(String::valueOf)
                .toList();
    }
}
