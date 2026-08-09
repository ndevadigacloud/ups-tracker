package com.ups.facility.config;

import java.util.Collection;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
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
        // Logs the fully-resolved consumer config (including the actual
        // bootstrap.servers value the client will connect to) once, at
        // container startup - rules out "env var name was wrong and it's
        // silently still pointed at localhost:9092" without needing to guess.
        factory.getContainerProperties().setLogContainerConfig(true);
        // Emits a ListenerContainerIdleEvent every 30s with no messages - a
        // heartbeat proving the poll loop is alive even when there's nothing
        // to consume, vs. a container that's hung and never polling at all.
        factory.getContainerProperties().setIdleEventInterval(30_000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "Giving up on record after retries - topic={}, partition={}, offset={}, key={}, "
                                + "value={}: {}",
                        record.topic(), record.partition(), record.offset(), record.key(), record.value(),
                        exception.getMessage(), exception),
                new FixedBackOff(1000L, 2)) {
            @Override
            public void handleOtherException(Exception thrownException, Consumer<?, ?> consumer,
                                              MessageListenerContainer container, boolean batchListener) {
                // Fires for failures NOT tied to a specific record - e.g. the
                // consumer can't register/authenticate/reach the broker at all
                // during poll(). This is the "can't register as a consumer"
                // case: without this override, that error only ever goes to
                // Spring Kafka's own logger, which application.yml keeps quiet.
                log.error("Kafka consumer error not tied to a specific record (connectivity/registration "
                        + "failure during poll): {}", thrownException.getMessage(), thrownException);
                super.handleOtherException(thrownException, consumer, container, batchListener);
            }
        };
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
