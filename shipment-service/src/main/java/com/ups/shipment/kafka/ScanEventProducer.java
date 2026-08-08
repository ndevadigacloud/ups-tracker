package com.ups.shipment.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ups.shipment.dto.ScanEvent;

@Component
public class ScanEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ScanEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${ups.kafka.topic.scan-event}")
    private String scanEventTopic;

    public ScanEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(ScanEvent event) {
        log.info("Publishing simulated ScanEvent for shipment {}: type={}, location={}",
                event.getShipmentId(), event.getEventType(), event.getLocation());
        kafkaTemplate.send(scanEventTopic, event.getShipmentId(), event);
    }
}
