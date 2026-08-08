package com.ups.shipment.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ups.shipment.model.TrackingEvent;

public interface TrackingEventRepository extends MongoRepository<TrackingEvent, String> {
    List<TrackingEvent> findByShipmentIdOrderByTimestampAsc(String shipmentId);
}
