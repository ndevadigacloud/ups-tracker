package com.ups.facility.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ups.facility.model.Facility;
import com.ups.facility.repository.FacilityRepository;

@RestController
@RequestMapping("/api/facilities")
@CrossOrigin(origins = "*")
public class FacilityController {

    private static final Logger log = LoggerFactory.getLogger(FacilityController.class);

    private final FacilityRepository facilityRepository;

    public FacilityController(FacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }

    @GetMapping
    public List<Facility> listFacilities() {
        return facilityRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Facility> createFacility(@RequestBody Facility facility) {
        facility.setId(null);
        facility.setCurrentLoadKg(0);
        Facility saved = facilityRepository.save(facility);
        log.info("Created facility {} ({}), capacity={} kg", saved.getId(), saved.getName(), saved.getCapacityKg());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Facility> getFacility(@PathVariable String id) {
        return facilityRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Facility {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }
}
