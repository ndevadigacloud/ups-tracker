package com.ups.shipment.kafka;

import java.time.Instant;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ups.shipment.model.ShipmentStatus;
import com.ups.shipment.model.TrackingEvent;
import com.ups.shipment.repository.ShipmentRepository;
import com.ups.shipment.repository.TrackingEventRepository;
import com.ups.shipment.sse.ShipmentSseHub;

@Component
public class ScanEventConsumer {

    private final TrackingEventRepository trackingEventRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentSseHub sseHub;

    public ScanEventConsumer(TrackingEventRepository trackingEventRepository,
                              ShipmentRepository shipmentRepository,
                              ShipmentSseHub sseHub) {
        this.trackingEventRepository = trackingEventRepository;
        this.shipmentRepository = shipmentRepository;
        this.sseHub = sseHub;
    }

    @KafkaListener(topics = "${ups.kafka.topic.scan-event}")
    public void onScanEvent(com.ups.shipment.dto.ScanEvent event) {
        TrackingEvent trackingEvent = new TrackingEvent(
                null, event.getShipmentId(), event.getEventType(), event.getLocation(),
                event.getDescription(), Instant.now());
        trackingEventRepository.save(trackingEvent);

        shipmentRepository.findById(event.getShipmentId()).ifPresent(shipment -> {
            ShipmentStatus newStatus = mapStatus(event.getEventType());
            if (newStatus != null) {
                shipment.setStatus(newStatus);
                shipmentRepository.save(shipment);
            }
        });

        sseHub.publish(event.getShipmentId(), "tracking-event", trackingEvent);
    }

    private ShipmentStatus mapStatus(String eventType) {
        return switch (eventType) {
            case "DEPARTED_HUB", "ARRIVED_AT_HUB" -> ShipmentStatus.IN_TRANSIT;
            case "OUT_FOR_DELIVERY" -> ShipmentStatus.OUT_FOR_DELIVERY;
            case "DELIVERED" -> ShipmentStatus.DELIVERED;
            case "DELIVERY_EXCEPTION" -> ShipmentStatus.EXCEPTION;
            default -> null;
        };
    }
}
