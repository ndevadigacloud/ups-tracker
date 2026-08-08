package com.ups.shipment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShipmentServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(ShipmentServiceApplication.class);

    public static void main(String[] args) {
        log.info("Starting shipment-service...");
        SpringApplication.run(ShipmentServiceApplication.class, args);
        log.info("shipment-service started and ready to accept requests");
    }
}
