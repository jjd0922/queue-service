package com.queue.infrastructure.queue.redis.adapter;

import com.queue.application.dto.EnqueueCommand;
import com.queue.application.dto.PromoteCommand;
import com.queue.application.dto.PromoteResult;
import com.queue.application.port.out.QueueCommandPort;
import com.queue.domain.model.EnqueueDecision;
import com.queue.domain.model.EnqueueOutcome;
import com.queue.domain.model.QueueEntry;
import com.queue.domain.model.QueueEntryStatus;
import com.queue.infrastructure.queue.redis.generator.RedisQueueKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class RedisQueueCommandAdapter implements QueueCommandPort {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<List> enqueueOrGetExistingScript;
    private final RedisScript<Long> promoteWaitingEntriesScript;
    private final RedisQueueKeyGenerator keyGenerator;

    @Override
    public EnqueueDecision enqueueOrGetExisting(EnqueueCommand request) {
        Instant now = request.requestedAt();
        String token = UUID.randomUUID().toString();

        List<String> keys = List.of(
                keyGenerator.userIndexKey(request.queueId(), request.userId()),
                keyGenerator.sequenceKey(request.queueId()),
                keyGenerator.entryKey(token),
                keyGenerator.waitingQueueKey(request.queueId())
        );

        List<String> args = List.of(
                keyGenerator.entryKeyPrefix(),
                token,
                request.queueId(),
                String.valueOf(request.userId()),
                QueueEntryStatus.WAITING.name(),
                now.toString(),
                now.toString()
        );

        List<?> result = stringRedisTemplate.execute(
                enqueueOrGetExistingScript,
                keys,
                args.toArray()
        );

        return mapResult(request, result);
    }

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

        int actualPromotedCount = promotedCount == null ? 0 : promotedCount.intValue();

        return new PromoteResult(
                request.queueId(),
                request.promoteBatchSize(),
                actualPromotedCount
        );
    }

    private EnqueueDecision mapResult(EnqueueCommand request, List<?> result) {
        if (result == null || result.size() < 8) {
            throw new IllegalStateException("Redis enqueue script returned invalid result.");
        }

        String createdFlag = stringValue(result.get(0));
        String token = stringValue(result.get(1));
        String statusValue = stringValue(result.get(2));
        Long sequence = Long.parseLong(stringValue(result.get(3)));
        Instant enteredAt = Instant.parse(stringValue(result.get(4)));
        Instant activatedAt = parseNullableInstant(result.get(5));
        Instant expiresAt = parseNullableInstant(result.get(6));
        Instant lastUpdatedAt = Instant.parse(stringValue(result.get(7)));

        QueueEntry entry = QueueEntry.restore(
                token,
                request.queueId(),
                request.userId(),
                QueueEntryStatus.valueOf(statusValue),
                sequence,
                enteredAt,
                activatedAt,
                expiresAt,
                lastUpdatedAt
        );

        if ("1".equals(createdFlag)) {
            return new EnqueueDecision(EnqueueOutcome.CREATED, entry);
        }

        if (entry.isActive()) {
            return new EnqueueDecision(EnqueueOutcome.ALREADY_ACTIVE, entry);
        }

        return new EnqueueDecision(EnqueueOutcome.ALREADY_WAITING, entry);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Instant parseNullableInstant(Object value) {
        String text = stringValue(value);
        if (text.isBlank()) {
            return null;
        }
        return Instant.parse(text);
    }
}