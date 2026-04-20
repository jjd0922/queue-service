package com.queue.infrastructure.queue.kafka.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueueKafkaPropertiesTest {

    @Test
    void resolveLifecycleDltTopic_returnsConfiguredTopic_whenPresent() {
        QueueKafkaProperties properties = new QueueKafkaProperties();
        properties.setLifecycleTopic("queue.lifecycle.v1");
        properties.setLifecycleDltTopic("queue.lifecycle.v1.custom-dlt");

        assertThat(properties.resolveLifecycleDltTopic()).isEqualTo("queue.lifecycle.v1.custom-dlt");
    }

    @Test
    void resolveLifecycleDltTopic_returnsDerivedTopic_whenNotConfigured() {
        QueueKafkaProperties properties = new QueueKafkaProperties();
        properties.setLifecycleTopic("queue.lifecycle.v1");

        assertThat(properties.resolveLifecycleDltTopic()).isEqualTo("queue.lifecycle.v1.dlt");
    }
}
