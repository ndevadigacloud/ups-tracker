package com.ups.shipment.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private Map<String, Long> countsByStatus;
    private List<Map<String, Object>> volumeByDay;
    private List<Map<String, Object>> topDestinationFacilities;
}
