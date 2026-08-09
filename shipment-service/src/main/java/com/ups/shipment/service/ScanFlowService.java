package com.ups.shipment.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ups.shipment.dto.NextScanStepResponse;
import com.ups.shipment.model.Shipment;
import com.ups.shipment.model.ShipmentStatus;
import com.ups.shipment.model.TrackingEvent;
import com.ups.shipment.repository.TrackingEventRepository;

/**
 * Owns the "what's the single realistic next scan for this shipment" state
 * machine, so the UI never has to guess (or let a user click steps out of
 * order) - it just asks this service and renders whatever comes back.
 *
 * The scan sequence is derived from the *last recorded scan event*, not just
 * the shipment's status, because ARRIVED_AT_HUB and DEPARTED_HUB both map to
 * the same IN_TRANSIT status - status alone can't tell them apart.
 */
@Service
public class ScanFlowService {

    private static final Logger log = LoggerFactory.getLogger(ScanFlowService.class);

    private static final List<String> SCAN_SEQUENCE = List.of(
            "ARRIVED_AT_HUB", "DEPARTED_HUB", "OUT_FOR_DELIVERY", "DELIVERED");

    private final TrackingEventRepository trackingEventRepository;

    public ScanFlowService(TrackingEventRepository trackingEventRepository) {
        this.trackingEventRepository = trackingEventRepository;
    }

    public NextScanStepResponse nextStep(Shipment shipment) {
        if (shipment.getStatus() == ShipmentStatus.CREATED) {
            return new NextScanStepResponse(null, "PENDING_CAPACITY",
                    "Waiting on facility-service to confirm capacity before this shipment can move.");
        }
        if (shipment.getStatus() == ShipmentStatus.CAPACITY_REJECTED) {
            return new NextScanStepResponse(null, "REJECTED",
                    "Origin facility was at capacity - this shipment needs to be rebooked.");
        }
        if (shipment.getStatus() == ShipmentStatus.EXCEPTION) {
            return new NextScanStepResponse(null, "EXCEPTION", "Delivery exception - manual intervention required.");
        }
        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            return new NextScanStepResponse(null, "DELIVERED", "Delivered.");
        }

        List<TrackingEvent> events = trackingEventRepository.findByShipmentIdOrderByTimestampAsc(shipment.getId());
        String lastEventType = events.isEmpty() ? null : events.get(events.size() - 1).getEventType();

        int nextIndex = lastEventType == null ? 0 : SCAN_SEQUENCE.indexOf(lastEventType) + 1;
        if (nextIndex <= 0 && lastEventType != null && !SCAN_SEQUENCE.contains(lastEventType)) {
            log.warn("Shipment {} has an unrecognized last scan type '{}', treating as start of sequence",
                    shipment.getId(), lastEventType);
            nextIndex = 0;
        }

        if (nextIndex >= SCAN_SEQUENCE.size()) {
            return new NextScanStepResponse(null, "DELIVERED", "Delivered.");
        }

        String next = SCAN_SEQUENCE.get(nextIndex);
        log.debug("Shipment {}: last scan={}, next expected scan={}", shipment.getId(), lastEventType, next);
        return new NextScanStepResponse(next, "READY", null);
    }
}
