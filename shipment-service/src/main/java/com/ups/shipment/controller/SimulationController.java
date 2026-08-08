package com.ups.shipment.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ups.shipment.dto.ScanEvent;
import com.ups.shipment.kafka.ScanEventProducer;

/**
 * Stand-in for a real driver/hub-scanner app: lets the demo UI push a scan
 * event for a shipment so the tracking timeline updates end-to-end via Kafka.
 */
@RestController
@RequestMapping("/api/simulate")
@CrossOrigin(origins = "*")
public class SimulationController {

    private final ScanEventProducer scanEventProducer;

    public SimulationController(ScanEventProducer scanEventProducer) {
        this.scanEventProducer = scanEventProducer;
    }

    @PostMapping("/shipments/{id}/scan")
    public void simulateScan(@PathVariable String id, @RequestBody ScanEvent event) {
        event.setShipmentId(id);
        scanEventProducer.publish(event);
    }
}
