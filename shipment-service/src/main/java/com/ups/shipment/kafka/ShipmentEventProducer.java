package com.ups.shipment.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ups.shipment.dto.ShipmentCreatedEvent;

@Component
public class ShipmentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${ups.kafka.topic.shipment-created}")
    private String shipmentCreatedTopic;

    public ShipmentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishShipmentCreated(ShipmentCreatedEvent event) {
        kafkaTemplate.send(shipmentCreatedTopic, event.getShipmentId(), event);
    }
}
