package com.ups.shipment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScanEvent {
    private String shipmentId;
    private String eventType;
    private String location;
    private String description;
}
