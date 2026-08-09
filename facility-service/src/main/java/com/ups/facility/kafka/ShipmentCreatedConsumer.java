package com.ups.facility.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ups.facility.dto.CapacityResultEvent;
import com.ups.facility.dto.ShipmentCreatedEvent;
import com.ups.facility.service.CapacityService;

import jakarta.annotation.PostConstruct;

@Component
public class ShipmentCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(ShipmentCreatedConsumer.class);

    private final CapacityService capacityService;
    private final CapacityResultProducer capacityResultProducer;

    @Value("${ups.kafka.topic.shipment-created}")
    private String shipmentCreatedTopic;

    public ShipmentCreatedConsumer(CapacityService capacityService, CapacityResultProducer capacityResultProducer) {
        this.capacityService = capacityService;
        this.capacityResultProducer = capacityResultProducer;
    }

    @PostConstruct
    void logRegistration() {
        // Confirms the Spring bean itself was created and wired successfully.
        // If this line is missing from the logs entirely, the problem is a
        // Spring context/wiring failure, not a Kafka connectivity issue -
        // check for a startup exception before this point instead.
        log.info("ShipmentCreatedConsumer registered, listening on topic '{}'", shipmentCreatedTopic);
    }

    @KafkaListener(topics = "${ups.kafka.topic.shipment-created}")
    public void onShipmentCreated(ShipmentCreatedEvent event) {
        log.info("Consumed ShipmentCreatedEvent: shipmentId={}, originFacilityId={}, weightKg={}",
                event.getShipmentId(), event.getOriginFacilityId(), event.getWeightKg());

        boolean reserved = capacityService.tryReserve(event.getOriginFacilityId(), event.getWeightKg());

        CapacityResultEvent result = reserved
                ? new CapacityResultEvent(event.getShipmentId(), true, null)
                : new CapacityResultEvent(event.getShipmentId(), false, "Facility at capacity");

        if (reserved) {
            log.info("Capacity reserved for shipment {} at facility {}", event.getShipmentId(), event.getOriginFacilityId());
        } else {
            log.warn("Capacity rejected for shipment {} at facility {}: facility at capacity",
                    event.getShipmentId(), event.getOriginFacilityId());
        }

        capacityResultProducer.publish(result);
    }
}
