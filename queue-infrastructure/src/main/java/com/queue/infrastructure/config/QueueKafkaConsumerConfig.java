package com.queue.infrastructure.config;

import com.queue.infrastructure.queue.kafka.QueueKafkaProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class QueueKafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> queueLifecycleKafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            CommonErrorHandler queueLifecycleCommonErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(queueLifecycleCommonErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    @Bean
    public CommonErrorHandler queueLifecycleCommonErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate,
            QueueKafkaProperties queueKafkaProperties
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> dltPartition(record, queueKafkaProperties)
        );

        long retries = Math.max(queueKafkaProperties.getConsumerMaxAttempts() - 1L, 0L);
        FixedBackOff backOff = new FixedBackOff(queueKafkaProperties.getConsumerBackoffMs(), retries);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(DeserializationException.class, IllegalArgumentException.class);
        errorHandler.setCommitRecovered(true);
        return errorHandler;
    }

    private TopicPartition dltPartition(ConsumerRecord<?, ?> record, QueueKafkaProperties queueKafkaProperties) {
        return new TopicPartition(queueKafkaProperties.resolveLifecycleDltTopic(), record.partition());
    }
}
