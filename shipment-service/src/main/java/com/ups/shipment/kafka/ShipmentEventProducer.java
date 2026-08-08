package com.ups.shipment.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ups.shipment.dto.ShipmentCreatedEvent;

@Component
public class ShipmentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ShipmentEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${ups.kafka.topic.shipment-created}")
    private String shipmentCreatedTopic;

    public ShipmentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishShipmentCreated(ShipmentCreatedEvent event) {
        log.info("Publishing ShipmentCreatedEvent to topic '{}': shipmentId={}, originFacilityId={}, weightKg={}",
                shipmentCreatedTopic, event.getShipmentId(), event.getOriginFacilityId(), event.getWeightKg());

        kafkaTemplate.send(shipmentCreatedTopic, event.getShipmentId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ShipmentCreatedEvent for shipment {}", event.getShipmentId(), ex);
                    } else {
                        log.debug("ShipmentCreatedEvent for shipment {} written to partition {} offset {}",
                                event.getShipmentId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
