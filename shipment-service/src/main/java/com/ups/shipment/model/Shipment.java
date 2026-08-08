package com.ups.shipment.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "shipments")
public class Shipment {

    @Id
    private String id;

    private Address sender;
    private Address receiver;

    private double weightKg;
    private ServiceLevel serviceLevel;

    @Indexed
    private ShipmentStatus status = ShipmentStatus.CREATED;

    private String originFacilityId;
    private String destinationFacilityId;

    private Instant estimatedDelivery;

    @CreatedDate
    @Indexed
    private Instant createdAt;
}
