package com.ups.shipment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.ups.shipment.model.Shipment;
import com.ups.shipment.model.ShipmentStatus;

public interface ShipmentRepository extends MongoRepository<Shipment, String> {
    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);
}
