package com.ups.facility.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ups.facility.model.Facility;
import com.ups.facility.repository.FacilityRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private final FacilityRepository facilityRepository;

    public DataSeeder(FacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }

    @Override
    public void run(String... args) {
        if (facilityRepository.count() > 0) {
            return;
        }
        facilityRepository.save(new Facility(null, "Louisville Air Hub", "Louisville", 50_000, 0));
        facilityRepository.save(new Facility(null, "Secaucus Ground Hub", "Secaucus", 30_000, 0));
        facilityRepository.save(new Facility(null, "Ontario Regional Hub", "Ontario", 40_000, 0));
    }
}
