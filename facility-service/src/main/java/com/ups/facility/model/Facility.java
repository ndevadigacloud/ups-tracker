package com.ups.facility.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "facilities")
public class Facility {

    @Id
    private String id;

    private String name;
    private String city;

    /** Max combined weight (kg) the facility can hold in its current processing window. */
    private double capacityKg;

    /** Weight (kg) currently reserved against this facility's capacity. */
    private double currentLoadKg;
}
