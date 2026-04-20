package com.queue.infrastructure.queue.kafka.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueueLifecycleConsumerMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private QueueLifecycleConsumerMetrics metrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metrics = new QueueLifecycleConsumerMetrics(meterRegistry);
    }

    @Test
    void incrementsCountersAndLagGauge() {
        metrics.incrementConsumed();
        metrics.incrementSuccess();
        metrics.incrementFailure();
        metrics.incrementDuplicateIgnored();
        metrics.incrementRetry();
        metrics.incrementRetryExhausted();
        metrics.incrementDltPublished();
        metrics.recordLag("queue.lifecycle.v1", 0, "queue-lifecycle-audit-v1", 7);

        assertThat(meterRegistry.get("queue_lifecycle_consumer_consumed_total").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("queue_lifecycle_consumer_success_total").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("queue_lifecycle_consumer_failure_total").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("queue_lifecycle_consumer_duplicate_ignored_total").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("queue_lifecycle_consumer_retry_total").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("queue_lifecycle_consumer_retry_exhausted_total").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("queue_lifecycle_consumer_dlt_published_total").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("queue_lifecycle_consumer_lag").gauge().value()).isEqualTo(7.0);
    }
}
