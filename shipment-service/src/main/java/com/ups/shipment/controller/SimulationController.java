package com.ups.shipment.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ups.shipment.dto.NextScanStepResponse;
import com.ups.shipment.dto.ScanEvent;
import com.ups.shipment.kafka.ScanEventProducer;
import com.ups.shipment.model.Shipment;
import com.ups.shipment.repository.ShipmentRepository;
import com.ups.shipment.service.ScanFlowService;

/**
 * Stand-in for a real driver/hub-scanner app: lets the demo UI push a scan
 * event for a shipment so the tracking timeline updates end-to-end via Kafka.
 * Requests are validated against ScanFlowService's state machine so a scan
 * can't be recorded out of order (e.g. "delivered" before "out for delivery"),
 * even if something other than the UI's single-button flow calls this.
 */
@RestController
@RequestMapping("/api/simulate")
@CrossOrigin(origins = "*")
public class SimulationController {

    private static final Logger log = LoggerFactory.getLogger(SimulationController.class);

    private final ScanEventProducer scanEventProducer;
    private final ShipmentRepository shipmentRepository;
    private final ScanFlowService scanFlowService;

    public SimulationController(ScanEventProducer scanEventProducer,
                                 ShipmentRepository shipmentRepository,
                                 ScanFlowService scanFlowService) {
        this.scanEventProducer = scanEventProducer;
        this.shipmentRepository = shipmentRepository;
        this.scanFlowService = scanFlowService;
    }

    @PostMapping("/shipments/{id}/scan")
    public ResponseEntity<?> simulateScan(@PathVariable String id, @RequestBody ScanEvent event) {
        Shipment shipment = shipmentRepository.findById(id).orElse(null);
        if (shipment == null) {
            return ResponseEntity.notFound().build();
        }

        NextScanStepResponse expected = scanFlowService.nextStep(shipment);
        if (!"READY".equals(expected.getState()) || !expected.getEventType().equals(event.getEventType())) {
            log.warn("Rejected out-of-order scan for shipment {}: requested={}, expected={}",
                    id, event.getEventType(), expected.getEventType());
            return ResponseEntity.badRequest().body(
                    "Expected next scan to be '" + expected.getEventType() + "', got '" + event.getEventType() + "'");
        }

        log.info("Simulate-scan request received for shipment {}: type={}", id, event.getEventType());
        event.setShipmentId(id);
        scanEventProducer.publish(event);
        return ResponseEntity.accepted().build();
    }
}
