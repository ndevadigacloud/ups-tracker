package com.ups.facility.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ups.facility.dto.CapacityResultEvent;
import com.ups.facility.dto.ShipmentCreatedEvent;
import com.ups.facility.service.CapacityService;

@Component
public class ShipmentCreatedConsumer {

    private final CapacityService capacityService;
    private final CapacityResultProducer capacityResultProducer;

    public ShipmentCreatedConsumer(CapacityService capacityService, CapacityResultProducer capacityResultProducer) {
        this.capacityService = capacityService;
        this.capacityResultProducer = capacityResultProducer;
    }

    @KafkaListener(topics = "${ups.kafka.topic.shipment-created}")
    public void onShipmentCreated(ShipmentCreatedEvent event) {
        boolean reserved = capacityService.tryReserve(event.getOriginFacilityId(), event.getWeightKg());

        CapacityResultEvent result = reserved
                ? new CapacityResultEvent(event.getShipmentId(), true, null)
                : new CapacityResultEvent(event.getShipmentId(), false, "Facility at capacity");

        capacityResultProducer.publish(result);
    }
}
