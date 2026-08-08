package com.ups.shipment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CapacityResultEvent {
    private String shipmentId;
    private boolean reserved;
    private String reason;
}
