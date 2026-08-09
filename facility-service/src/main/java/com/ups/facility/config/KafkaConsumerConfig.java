package com.ups.facility.config;

import java.util.Collection;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Overrides Spring Boot's auto-configured listener container factory to make
 * two otherwise-invisible failure modes loud instead of silent:
 *  1. Whether this consumer ever actually joins its group and gets partitions
 *     assigned - the org.springframework.kafka logger is set to WARN in
 *     application.yml (too noisy at INFO), so without this, "never connected"
 *     and "connected fine" look identical in the logs.
 *  2. The full detail of a record that fails to process after retries -
 *     without this, a bad record can silently stall the consumer forever.
 */
@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setConsumerRebalanceListener(new LoggingRebalanceListener());

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "Giving up on record after retries - topic={}, partition={}, offset={}, key={}, "
                                + "value={}: {}",
                        record.topic(), record.partition(), record.offset(), record.key(), record.value(),
                        exception.getMessage(), exception),
                new FixedBackOff(1000L, 2));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    private static class LoggingRebalanceListener implements ConsumerRebalanceListener {

        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
            log.info("Consumer group revoked partitions: {}", partitions);
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            log.info("Consumer group assigned partitions: {} - now actively polling", partitions);
        }
    }
}
