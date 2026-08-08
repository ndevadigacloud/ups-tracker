package com.ups.shipment.kafka;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ups.shipment.model.ShipmentStatus;
import com.ups.shipment.model.TrackingEvent;
import com.ups.shipment.repository.ShipmentRepository;
import com.ups.shipment.repository.TrackingEventRepository;
import com.ups.shipment.sse.ShipmentSseHub;

@Component
public class ScanEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ScanEventConsumer.class);

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
        log.info("Consumed ScanEvent for shipment {}: type={}, location={}",
                event.getShipmentId(), event.getEventType(), event.getLocation());

        TrackingEvent trackingEvent = new TrackingEvent(
                null, event.getShipmentId(), event.getEventType(), event.getLocation(),
                event.getDescription(), Instant.now());
        trackingEventRepository.save(trackingEvent);
        log.debug("Persisted tracking event {} for shipment {}", trackingEvent.getId(), event.getShipmentId());

        shipmentRepository.findById(event.getShipmentId()).ifPresentOrElse(shipment -> {
            ShipmentStatus newStatus = mapStatus(event.getEventType());
            if (newStatus != null) {
                ShipmentStatus previous = shipment.getStatus();
                shipment.setStatus(newStatus);
                shipmentRepository.save(shipment);
                log.info("Shipment {} status transitioned {} -> {} (from scan {})",
                        event.getShipmentId(), previous, newStatus, event.getEventType());
            }
        }, () -> log.warn("Received scan event for unknown shipment {}", event.getShipmentId()));

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
