package com.ups.shipment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${ups.kafka.topic.shipment-created}")
    private String shipmentCreatedTopic;

    @Bean
    public NewTopic shipmentCreatedTopic() {
        return TopicBuilder.name(shipmentCreatedTopic).partitions(3).replicas(1).build();
    }
}
