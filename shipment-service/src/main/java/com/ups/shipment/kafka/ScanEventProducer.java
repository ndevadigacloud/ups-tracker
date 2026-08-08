package com.ups.shipment.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ups.shipment.dto.ScanEvent;

@Component
public class ScanEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${ups.kafka.topic.scan-event}")
    private String scanEventTopic;

    public ScanEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(ScanEvent event) {
        kafkaTemplate.send(scanEventTopic, event.getShipmentId(), event);
    }
}
