package com.queue.infrastructure.queue.kafka;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class QueueLifecycleConsumerMetrics {

    private final MeterRegistry meterRegistry;

    private final Counter consumedTotalCounter;
    private final Counter successTotalCounter;
    private final Counter failureTotalCounter;
    private final Counter duplicateIgnoredTotalCounter;
    private final Counter dltPublishedTotalCounter;
    private final Counter retryTotalCounter;
    private final Counter retryExhaustedTotalCounter;
    private final Timer processingLatencyTimer;

    private final Map<String, AtomicLong> lagByPartition = new ConcurrentHashMap<>();

    public QueueLifecycleConsumerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.consumedTotalCounter = Counter.builder("queue_lifecycle_consumer_consumed_total")
                .description("Total consumed queue lifecycle events")
                .register(meterRegistry);
        this.successTotalCounter = Counter.builder("queue_lifecycle_consumer_success_total")
                .description("Total successfully processed queue lifecycle events")
                .register(meterRegistry);
        this.failureTotalCounter = Counter.builder("queue_lifecycle_consumer_failure_total")
                .description("Total failed queue lifecycle events")
                .register(meterRegistry);
        this.duplicateIgnoredTotalCounter = Counter.builder("queue_lifecycle_consumer_duplicate_ignored_total")
                .description("Total duplicate queue lifecycle events ignored")
                .register(meterRegistry);
        this.dltPublishedTotalCounter = Counter.builder("queue_lifecycle_consumer_dlt_published_total")
                .description("Total queue lifecycle events published to DLT")
                .register(meterRegistry);
        this.retryTotalCounter = Counter.builder("queue_lifecycle_consumer_retry_total")
                .description("Total queue lifecycle consumer retries")
                .register(meterRegistry);
        this.retryExhaustedTotalCounter = Counter.builder("queue_lifecycle_consumer_retry_exhausted_total")
                .description("Total queue lifecycle consumer retry-exhausted events")
                .register(meterRegistry);
        this.processingLatencyTimer = Timer.builder("queue_lifecycle_consumer_processing_latency")
                .description("Queue lifecycle consumer processing latency")
                .register(meterRegistry);
    }

    public void incrementConsumed() {
        consumedTotalCounter.increment();
    }

    public void incrementSuccess() {
        successTotalCounter.increment();
    }

    public void incrementFailure() {
        failureTotalCounter.increment();
    }

    public void incrementDuplicateIgnored() {
        duplicateIgnoredTotalCounter.increment();
    }

    public void incrementDltPublished() {
        dltPublishedTotalCounter.increment();
    }

    public void incrementRetry() {
        retryTotalCounter.increment();
    }

    public void incrementRetryExhausted() {
        retryExhaustedTotalCounter.increment();
    }

    public void recordProcessingLatencyNanos(long latencyNanos) {
        if (latencyNanos >= 0) {
            processingLatencyTimer.record(latencyNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
        }
    }

    public void recordLag(String topic, int partition, String groupId, long lag) {
        String key = topic + ":" + partition + ":" + groupId;
        AtomicLong holder = lagByPartition.computeIfAbsent(key, ignored -> registerLagGauge(topic, partition, groupId));
        holder.set(Math.max(lag, 0L));
    }

    private AtomicLong registerLagGauge(String topic, int partition, String groupId) {
        AtomicLong holder = new AtomicLong(0L);
        Gauge.builder("queue_lifecycle_consumer_lag", holder, AtomicLong::get)
                .description("Latest queue lifecycle consumer lag per partition")
                .tag("topic", topic)
                .tag("partition", String.valueOf(partition))
                .tag("groupId", groupId)
                .register(meterRegistry);
        return holder;
    }
}
