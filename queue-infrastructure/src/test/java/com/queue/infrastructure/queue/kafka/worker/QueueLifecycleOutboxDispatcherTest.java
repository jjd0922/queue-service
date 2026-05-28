package com.queue.infrastructure.queue.kafka.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.queue.infrastructure.config.QueueLifecycleOutboxProperties;
import com.queue.infrastructure.queue.kafka.adapter.KafkaQueueLifecycleEventPublisher;
import com.queue.infrastructure.queue.kafka.adapter.QueueLifecycleOutboxDispatchJdbcAdapter;
import com.queue.infrastructure.queue.kafka.metrics.QueueLifecycleOutboxMetrics;
import com.queue.infrastructure.queue.kafka.model.QueueLifecycleEventMessage;
import com.queue.infrastructure.queue.kafka.outbox.QueueLifecycleOutboxEvent;
import com.queue.infrastructure.queue.kafka.outbox.QueueLifecycleOutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class QueueLifecycleOutboxDispatcherTest {

    private QueueLifecycleOutboxDispatchJdbcAdapter outboxAdapter;
    private KafkaQueueLifecycleEventPublisher publisher;
    private QueueLifecycleOutboxProperties properties;
    private QueueLifecycleOutboxMetrics metrics;
    private ObjectMapper objectMapper;
    private Clock clock;
    private QueueLifecycleOutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        outboxAdapter = mock(QueueLifecycleOutboxDispatchJdbcAdapter.class);
        publisher = mock(KafkaQueueLifecycleEventPublisher.class);
        properties = new QueueLifecycleOutboxProperties();
        properties.setEnabled(true);
        properties.setBatchSize(10);
        properties.setMaxRetryCount(3);
        properties.setRetryBackoffMs(1000L);
        properties.setPublishTimeoutMs(1000L);
        metrics = mock(QueueLifecycleOutboxMetrics.class);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        clock = Clock.fixed(Instant.parse("2026-04-18T01:00:00Z"), ZoneOffset.UTC);

        dispatcher = new QueueLifecycleOutboxDispatcher(
                outboxAdapter,
                publisher,
                properties,
                metrics,
                objectMapper,
                clock
        );
    }

    @Test
    void execute_publishesClaimedOutboxEventAndMarksPublished() throws Exception {
        QueueLifecycleOutboxEvent event = event(1L, 0);
        when(outboxAdapter.findDispatchable(10, Instant.parse("2026-04-18T01:00:00Z")))
                .thenReturn(List.of(event));
        when(outboxAdapter.claim(1L)).thenReturn(true);
        when(publisher.publish(any(QueueLifecycleEventMessage.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        dispatcher.execute();

        verify(outboxAdapter).claim(1L);
        verify(publisher).publish(any(QueueLifecycleEventMessage.class));
        verify(outboxAdapter).markPublished(1L, Instant.parse("2026-04-18T01:00:00Z"));
        verify(metrics).incrementFetched(1L);
        verify(metrics).incrementClaimed();
        verify(metrics).incrementPublished();
    }

    @Test
    void execute_marksFailedWhenPublishFails() throws Exception {
        QueueLifecycleOutboxEvent event = event(1L, 0);
        when(outboxAdapter.findDispatchable(10, Instant.parse("2026-04-18T01:00:00Z")))
                .thenReturn(List.of(event));
        when(outboxAdapter.claim(1L)).thenReturn(true);
        when(publisher.publish(any(QueueLifecycleEventMessage.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("kafka down")));
        when(outboxAdapter.markFailed(
                eq(event),
                eq(Instant.parse("2026-04-18T01:00:00Z")),
                eq(3),
                eq(1000L),
                eq("kafka down")
        )).thenReturn(QueueLifecycleOutboxStatus.FAILED);

        dispatcher.execute();

        verify(outboxAdapter).markFailed(
                eq(event),
                eq(Instant.parse("2026-04-18T01:00:00Z")),
                eq(3),
                eq(1000L),
                eq("kafka down")
        );
        verify(metrics).incrementFailed();
        verify(metrics, never()).incrementDead();
    }

    @Test
    void execute_recordsDeadMetricWhenMaxRetryExceeded() throws Exception {
        QueueLifecycleOutboxEvent event = event(1L, 2);
        when(outboxAdapter.findDispatchable(10, Instant.parse("2026-04-18T01:00:00Z")))
                .thenReturn(List.of(event));
        when(outboxAdapter.claim(1L)).thenReturn(true);
        when(publisher.publish(any(QueueLifecycleEventMessage.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("kafka down")));
        when(outboxAdapter.markFailed(any(), any(), anyInt(), anyLong(), anyString()))
                .thenReturn(QueueLifecycleOutboxStatus.DEAD);

        dispatcher.execute();

        verify(metrics).incrementFailed();
        verify(metrics).incrementDead();
    }

    @Test
    void execute_skipsEventWhenClaimFails() throws Exception {
        QueueLifecycleOutboxEvent event = event(1L, 0);
        when(outboxAdapter.findDispatchable(10, Instant.parse("2026-04-18T01:00:00Z")))
                .thenReturn(List.of(event));
        when(outboxAdapter.claim(1L)).thenReturn(false);

        dispatcher.execute();

        verifyNoInteractions(publisher);
        verify(metrics).incrementClaimSkipped();
    }

    @Test
    void execute_doesNothingWhenDisabled() {
        properties.setEnabled(false);

        dispatcher.execute();

        verifyNoInteractions(outboxAdapter, publisher, metrics);
    }

    private QueueLifecycleOutboxEvent event(Long id, int retryCount) throws Exception {
        QueueLifecycleEventMessage message = QueueLifecycleEventMessage.of(
                "event-1",
                "ENTERED",
                "token-1",
                1L,
                "WAITING",
                1L,
                Instant.parse("2026-04-18T01:00:00Z"),
                null
        );
        return new QueueLifecycleOutboxEvent(
                id,
                "event-1",
                "ENTERED",
                "token-1",
                1L,
                "WAITING",
                1L,
                Instant.parse("2026-04-18T01:00:00Z"),
                null,
                objectMapper.writeValueAsString(message),
                retryCount
        );
    }
}
