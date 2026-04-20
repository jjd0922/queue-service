package com.queue.infrastructure.config;

import com.queue.infrastructure.queue.kafka.config.QueueKafkaProperties;
import com.queue.infrastructure.queue.kafka.metrics.QueueLifecycleConsumerMetrics;
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
import org.springframework.kafka.listener.RetryListener;
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
            QueueKafkaProperties queueKafkaProperties,
            QueueLifecycleConsumerMetrics queueLifecycleConsumerMetrics
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> dltPartition(record, queueKafkaProperties)
        ) {
            @Override
            public void accept(ConsumerRecord<?, ?> record, org.apache.kafka.clients.consumer.Consumer<?, ?> consumer, Exception exception) {
                queueLifecycleConsumerMetrics.incrementDltPublished();
                super.accept(record, consumer, exception);
            }
        };

        long retries = Math.max(queueKafkaProperties.getConsumerMaxAttempts() - 1L, 0L);
        FixedBackOff backOff = new FixedBackOff(queueKafkaProperties.getConsumerBackoffMs(), retries);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(DeserializationException.class, IllegalArgumentException.class);
        errorHandler.setCommitRecovered(true);
        errorHandler.setRetryListeners(new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
                if (deliveryAttempt > 1) {
                    queueLifecycleConsumerMetrics.incrementRetry();
                }
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception ex) {
                queueLifecycleConsumerMetrics.incrementRetryExhausted();
            }
        });
        return errorHandler;
    }

    private TopicPartition dltPartition(ConsumerRecord<?, ?> record, QueueKafkaProperties queueKafkaProperties) {
        return new TopicPartition(queueKafkaProperties.resolveLifecycleDltTopic(), record.partition());
    }
}
