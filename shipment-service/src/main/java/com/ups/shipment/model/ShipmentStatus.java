package com.ups.shipment.model;

public enum ShipmentStatus {
    CREATED,
    CAPACITY_RESERVED,
    CAPACITY_REJECTED,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    EXCEPTION
}
