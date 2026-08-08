package com.ups.facility.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ups.facility.model.Facility;

public interface FacilityRepository extends MongoRepository<Facility, String> {
}
