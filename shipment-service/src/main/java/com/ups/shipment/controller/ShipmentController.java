package com.ups.shipment.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ups.shipment.dto.CreateShipmentRequest;
import com.ups.shipment.dto.DashboardResponse;
import com.ups.shipment.dto.ShipmentCreatedEvent;
import com.ups.shipment.kafka.ShipmentEventProducer;
import com.ups.shipment.model.Shipment;
import com.ups.shipment.model.TrackingEvent;
import com.ups.shipment.repository.ShipmentRepository;
import com.ups.shipment.repository.TrackingEventRepository;
import com.ups.shipment.service.DashboardService;
import com.ups.shipment.sse.ShipmentSseHub;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/shipments")
@CrossOrigin(origins = "*")
public class ShipmentController {

    private static final Logger log = LoggerFactory.getLogger(ShipmentController.class);

    private static final Map<String, Integer> ESTIMATED_TRANSIT_DAYS = Map.of(
            "GROUND", 5, "AIR", 2, "EXPRESS", 1);

    private final ShipmentRepository shipmentRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final ShipmentEventProducer shipmentEventProducer;
    private final DashboardService dashboardService;
    private final ShipmentSseHub sseHub;

    public ShipmentController(ShipmentRepository shipmentRepository,
                               TrackingEventRepository trackingEventRepository,
                               ShipmentEventProducer shipmentEventProducer,
                               DashboardService dashboardService,
                               ShipmentSseHub sseHub) {
        this.shipmentRepository = shipmentRepository;
        this.trackingEventRepository = trackingEventRepository;
        this.shipmentEventProducer = shipmentEventProducer;
        this.dashboardService = dashboardService;
        this.sseHub = sseHub;
    }

    @PostMapping
    public ResponseEntity<Shipment> createShipment(@Valid @RequestBody CreateShipmentRequest request) {
        log.info("Received create-shipment request: origin={}, destination={}, weightKg={}, serviceLevel={}",
                request.getOriginFacilityId(), request.getDestinationFacilityId(),
                request.getWeightKg(), request.getServiceLevel());

        Shipment shipment = new Shipment();
        shipment.setSender(request.getSender());
        shipment.setReceiver(request.getReceiver());
        shipment.setWeightKg(request.getWeightKg());
        shipment.setServiceLevel(request.getServiceLevel());
        shipment.setOriginFacilityId(request.getOriginFacilityId());
        shipment.setDestinationFacilityId(request.getDestinationFacilityId());
        shipment.setCreatedAt(Instant.now());
        shipment.setEstimatedDelivery(Instant.now().plus(
                ESTIMATED_TRANSIT_DAYS.get(request.getServiceLevel().name()), ChronoUnit.DAYS));

        Shipment saved = shipmentRepository.save(shipment);
        log.info("Persisted shipment {} with status {}", saved.getId(), saved.getStatus());

        shipmentEventProducer.publishShipmentCreated(
                new ShipmentCreatedEvent(saved.getId(), saved.getOriginFacilityId(), saved.getWeightKg()));

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public Page<Shipment> listShipments(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        if (status != null) {
            return shipmentRepository.findByStatus(
                    com.ups.shipment.model.ShipmentStatus.valueOf(status.toUpperCase()), pageRequest);
        }
        return shipmentRepository.findAll(pageRequest);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Shipment> getShipment(@PathVariable String id) {
        return shipmentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/tracking-events")
    public List<TrackingEvent> getTrackingEvents(@PathVariable String id) {
        return trackingEventRepository.findByShipmentIdOrderByTimestampAsc(id);
    }

    @GetMapping(value = "/{id}/stream", produces = "text/event-stream")
    public SseEmitter streamShipmentUpdates(@PathVariable String id) {
        log.info("Browser subscribed to SSE stream for shipment {}", id);
        return sseHub.subscribe(id);
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard() {
        log.debug("Building dashboard aggregation");
        return dashboardService.buildDashboard();
    }
}
