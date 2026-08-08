package com.ups.shipment.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ups.shipment.dto.CapacityResultEvent;
import com.ups.shipment.model.Shipment;
import com.ups.shipment.model.ShipmentStatus;
import com.ups.shipment.repository.ShipmentRepository;
import com.ups.shipment.sse.ShipmentSseHub;

@Component
public class CapacityResultConsumer {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentSseHub sseHub;

    public CapacityResultConsumer(ShipmentRepository shipmentRepository, ShipmentSseHub sseHub) {
        this.shipmentRepository = shipmentRepository;
        this.sseHub = sseHub;
    }

    @KafkaListener(topics = "${ups.kafka.topic.capacity-reserved}")
    public void onCapacityReserved(CapacityResultEvent event) {
        updateStatus(event.getShipmentId(), ShipmentStatus.CAPACITY_RESERVED);
    }

    @KafkaListener(topics = "${ups.kafka.topic.capacity-rejected}")
    public void onCapacityRejected(CapacityResultEvent event) {
        updateStatus(event.getShipmentId(), ShipmentStatus.CAPACITY_REJECTED);
    }

    private void updateStatus(String shipmentId, ShipmentStatus status) {
        shipmentRepository.findById(shipmentId).ifPresent(shipment -> {
            shipment.setStatus(status);
            shipmentRepository.save(shipment);
            sseHub.publish(shipmentId, "status", shipment);
        });
    }
}
