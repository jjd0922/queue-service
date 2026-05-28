package com.queue.infrastructure.queue.kafka.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.queue.infrastructure.config.QueueLifecycleOutboxProperties;
import com.queue.infrastructure.queue.kafka.adapter.KafkaQueueLifecycleEventPublisher;
import com.queue.infrastructure.queue.kafka.adapter.QueueLifecycleOutboxDispatchJdbcAdapter;
import com.queue.infrastructure.queue.kafka.metrics.QueueLifecycleOutboxMetrics;
import com.queue.infrastructure.queue.kafka.model.QueueLifecycleEventMessage;
import com.queue.infrastructure.queue.kafka.outbox.QueueLifecycleOutboxEvent;
import com.queue.infrastructure.queue.kafka.outbox.QueueLifecycleOutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueLifecycleOutboxDispatcher {

    private final QueueLifecycleOutboxDispatchJdbcAdapter outboxAdapter;
    private final KafkaQueueLifecycleEventPublisher publisher;
    private final QueueLifecycleOutboxProperties properties;
    private final QueueLifecycleOutboxMetrics metrics;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${queue.lifecycle.outbox.fixed-delay-ms:1000}")
    public void execute() {
        if (!properties.isEnabled()) {
            return;
        }

        Instant now = Instant.now(clock);
        List<QueueLifecycleOutboxEvent> events = outboxAdapter.findDispatchable(properties.getBatchSize(), now);
        metrics.incrementFetched(events.size());

        for (QueueLifecycleOutboxEvent event : events) {
            dispatch(event);
        }
    }

    private void dispatch(QueueLifecycleOutboxEvent event) {
        long startedAtNanos = System.nanoTime();
        if (!outboxAdapter.claim(event.id())) {
            metrics.incrementClaimSkipped();
            return;
        }
        metrics.incrementClaimed();

        try {
            QueueLifecycleEventMessage message = deserialize(event.payload());
            publisher.publish(message).get(properties.getPublishTimeoutMs(), TimeUnit.MILLISECONDS);
            outboxAdapter.markPublished(event.id(), Instant.now(clock));
            metrics.incrementPublished();
        } catch (Exception e) {
            QueueLifecycleOutboxStatus nextStatus = outboxAdapter.markFailed(
                    event,
                    Instant.now(clock),
                    properties.getMaxRetryCount(),
                    properties.getRetryBackoffMs(),
                    rootMessage(e)
            );
            metrics.incrementFailed();
            if (nextStatus == QueueLifecycleOutboxStatus.DEAD) {
                metrics.incrementDead();
            }
            log.warn(
                    "queue lifecycle outbox dispatch failed. outboxId={}, eventId={}, eventType={}, nextStatus={}",
                    event.id(),
                    event.eventId(),
                    event.eventType(),
                    nextStatus,
                    e
            );
        } finally {
            metrics.recordDispatchLatencyNanos(System.nanoTime() - startedAtNanos);
        }
    }

    private QueueLifecycleEventMessage deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, QueueLifecycleEventMessage.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize queue lifecycle outbox payload.", e);
        }
    }

    private String rootMessage(Exception e) {
        Throwable current = e;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}
