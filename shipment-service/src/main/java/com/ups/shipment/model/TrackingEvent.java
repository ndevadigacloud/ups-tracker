package com.ups.shipment.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tracking_events")
public class TrackingEvent {

    @Id
    private String id;

    @Indexed
    private String shipmentId;

    private String eventType;
    private String location;
    private String description;
    private Instant timestamp;
}
