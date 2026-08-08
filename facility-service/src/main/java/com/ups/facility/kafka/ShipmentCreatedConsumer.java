package com.ups.facility.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ups.facility.dto.CapacityResultEvent;
import com.ups.facility.dto.ShipmentCreatedEvent;
import com.ups.facility.service.CapacityService;

@Component
public class ShipmentCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(ShipmentCreatedConsumer.class);

    private final CapacityService capacityService;
    private final CapacityResultProducer capacityResultProducer;

    public ShipmentCreatedConsumer(CapacityService capacityService, CapacityResultProducer capacityResultProducer) {
        this.capacityService = capacityService;
        this.capacityResultProducer = capacityResultProducer;
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
