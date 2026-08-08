package com.ups.facility.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ups.facility.dto.CapacityResultEvent;

@Component
public class CapacityResultProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${ups.kafka.topic.capacity-reserved}")
    private String capacityReservedTopic;

    @Value("${ups.kafka.topic.capacity-rejected}")
    private String capacityRejectedTopic;

    public CapacityResultProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(CapacityResultEvent event) {
        String topic = event.isReserved() ? capacityReservedTopic : capacityRejectedTopic;
        kafkaTemplate.send(topic, event.getShipmentId(), event);
    }
}
