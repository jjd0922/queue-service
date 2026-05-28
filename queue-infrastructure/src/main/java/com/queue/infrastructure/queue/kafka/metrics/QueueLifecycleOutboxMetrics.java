package com.queue.infrastructure.queue.kafka.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class QueueLifecycleOutboxMetrics {

    private final Counter fetchedTotalCounter;
    private final Counter claimedTotalCounter;
    private final Counter claimSkippedTotalCounter;
    private final Counter publishedTotalCounter;
    private final Counter failedTotalCounter;
    private final Counter deadTotalCounter;
    private final Timer dispatchLatencyTimer;

    public QueueLifecycleOutboxMetrics(MeterRegistry meterRegistry) {
        this.fetchedTotalCounter = Counter.builder("queue_lifecycle_outbox_fetched_total")
                .description("Total queue lifecycle outbox events fetched for dispatch")
                .register(meterRegistry);
        this.claimedTotalCounter = Counter.builder("queue_lifecycle_outbox_claimed_total")
                .description("Total queue lifecycle outbox events claimed for dispatch")
                .register(meterRegistry);
        this.claimSkippedTotalCounter = Counter.builder("queue_lifecycle_outbox_claim_skipped_total")
                .description("Total queue lifecycle outbox events skipped because claim failed")
                .register(meterRegistry);
        this.publishedTotalCounter = Counter.builder("queue_lifecycle_outbox_published_total")
                .description("Total queue lifecycle outbox events published to Kafka")
                .register(meterRegistry);
        this.failedTotalCounter = Counter.builder("queue_lifecycle_outbox_failed_total")
                .description("Total queue lifecycle outbox event publish failures")
                .register(meterRegistry);
        this.deadTotalCounter = Counter.builder("queue_lifecycle_outbox_dead_total")
                .description("Total queue lifecycle outbox events moved to DEAD")
                .register(meterRegistry);
        this.dispatchLatencyTimer = Timer.builder("queue_lifecycle_outbox_dispatch_latency")
                .description("Queue lifecycle outbox dispatch latency")
                .register(meterRegistry);
    }

    public void incrementFetched(long count) {
        if (count > 0) {
            fetchedTotalCounter.increment(count);
        }
    }

    public void incrementClaimed() {
        claimedTotalCounter.increment();
    }

    public void incrementClaimSkipped() {
        claimSkippedTotalCounter.increment();
    }

    public void incrementPublished() {
        publishedTotalCounter.increment();
    }

    public void incrementFailed() {
        failedTotalCounter.increment();
    }

    public void incrementDead() {
        deadTotalCounter.increment();
    }

    public void recordDispatchLatencyNanos(long latencyNanos) {
        if (latencyNanos >= 0) {
            dispatchLatencyTimer.record(latencyNanos, TimeUnit.NANOSECONDS);
        }
    }
}
