package com.ups.facility.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ups.facility.dto.CapacityResultEvent;

@Component
public class CapacityResultProducer {

    private static final Logger log = LoggerFactory.getLogger(CapacityResultProducer.class);

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
        log.info("Publishing CapacityResultEvent to topic '{}': shipmentId={}, reserved={}",
                topic, event.getShipmentId(), event.isReserved());
        kafkaTemplate.send(topic, event.getShipmentId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish CapacityResultEvent for shipment {}", event.getShipmentId(), ex);
                    }
                });
    }
}
