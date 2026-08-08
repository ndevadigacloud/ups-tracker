package com.ups.shipment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String name;
    private String street;
    private String city;
    private String state;
    private String zip;
}
