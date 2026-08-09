package com.ups.shipment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What a driver/hub scanner would realistically do next for this shipment,
 * or null eventType if there's nothing to scan yet (waiting on capacity) or
 * ever again (terminal state).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NextScanStepResponse {
    private String eventType;
    private String state;
    private String message;
}
