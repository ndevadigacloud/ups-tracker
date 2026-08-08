package com.ups.facility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FacilityServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(FacilityServiceApplication.class);

    public static void main(String[] args) {
        log.info("Starting facility-service...");
        SpringApplication.run(FacilityServiceApplication.class, args);
        log.info("facility-service started and ready to accept requests");
    }
}
