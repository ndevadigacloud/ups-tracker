package com.ups.shipment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaTopicConfig.class);

    @Value("${ups.kafka.topic.shipment-created}")
    private String shipmentCreatedTopic;

    @Bean
    public NewTopic shipmentCreatedTopic() {
        log.info("Declaring Kafka topic '{}' (3 partitions, replication factor 1)", shipmentCreatedTopic);
        return TopicBuilder.name(shipmentCreatedTopic).partitions(3).replicas(1).build();
    }
}
