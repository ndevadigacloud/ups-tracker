package com.ups.shipment.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ups.shipment.dto.CapacityResultEvent;
import com.ups.shipment.model.Shipment;
import com.ups.shipment.model.ShipmentStatus;
import com.ups.shipment.repository.ShipmentRepository;
import com.ups.shipment.sse.ShipmentSseHub;

@Component
public class CapacityResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(CapacityResultConsumer.class);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentSseHub sseHub;

    public CapacityResultConsumer(ShipmentRepository shipmentRepository, ShipmentSseHub sseHub) {
        this.shipmentRepository = shipmentRepository;
        this.sseHub = sseHub;
    }

    @KafkaListener(topics = "${ups.kafka.topic.capacity-reserved}")
    public void onCapacityReserved(CapacityResultEvent event) {
        log.info("Consumed CapacityResultEvent (reserved) for shipment {}", event.getShipmentId());
        updateStatus(event.getShipmentId(), ShipmentStatus.CAPACITY_RESERVED);
    }

    @KafkaListener(topics = "${ups.kafka.topic.capacity-rejected}")
    public void onCapacityRejected(CapacityResultEvent event) {
        log.warn("Consumed CapacityResultEvent (rejected) for shipment {}: reason={}",
                event.getShipmentId(), event.getReason());
        updateStatus(event.getShipmentId(), ShipmentStatus.CAPACITY_REJECTED);
    }

    private void updateStatus(String shipmentId, ShipmentStatus status) {
        shipmentRepository.findById(shipmentId).ifPresentOrElse(shipment -> {
            ShipmentStatus previous = shipment.getStatus();
            shipment.setStatus(status);
            shipmentRepository.save(shipment);
            log.info("Shipment {} status transitioned {} -> {}", shipmentId, previous, status);
            sseHub.publish(shipmentId, "status", shipment);
        }, () -> log.warn("Received capacity result for unknown shipment {}", shipmentId));
    }
}
