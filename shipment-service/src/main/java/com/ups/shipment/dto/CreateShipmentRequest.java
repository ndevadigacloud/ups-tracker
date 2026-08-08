package com.ups.shipment.dto;

import com.ups.shipment.model.Address;
import com.ups.shipment.model.ServiceLevel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateShipmentRequest {

    @NotNull @Valid
    private Address sender;

    @NotNull @Valid
    private Address receiver;

    @Positive
    private double weightKg;

    @NotNull
    private ServiceLevel serviceLevel;

    @NotNull
    private String originFacilityId;

    @NotNull
    private String destinationFacilityId;
}
