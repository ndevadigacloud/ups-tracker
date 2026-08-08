package com.ups.facility.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentCreatedEvent {
    private String shipmentId;
    private String originFacilityId;
    private double weightKg;
}
