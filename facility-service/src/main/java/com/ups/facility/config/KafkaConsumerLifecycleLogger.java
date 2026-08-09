package com.ups.facility.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.event.ConsumerFailedToStartEvent;
import org.springframework.kafka.event.ConsumerStartedEvent;
import org.springframework.kafka.event.ConsumerStartingEvent;
import org.springframework.kafka.event.ConsumerStoppedEvent;
import org.springframework.kafka.event.ListenerContainerIdleEvent;
import org.springframework.kafka.event.NonResponsiveConsumerEvent;
import org.springframework.stereotype.Component;

/**
 * Spring Kafka publishes these as application events regardless of the
 * org.springframework.kafka logger's level, so wiring them to our own
 * always-on logger surfaces consumer lifecycle problems that would otherwise
 * be invisible - most importantly ConsumerFailedToStartEvent, which is
 * exactly what fires when a consumer never manages to connect at all (no
 * exception bubbles up anywhere else in that case).
 */
@Component
public class KafkaConsumerLifecycleLogger {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerLifecycleLogger.class);

    @EventListener
    public void onStarting(ConsumerStartingEvent event) {
        log.info("Kafka consumer starting: {}", event);
    }

    @EventListener
    public void onStarted(ConsumerStartedEvent event) {
        log.info("Kafka consumer started successfully: {}", event);
    }

    @EventListener
    public void onFailedToStart(ConsumerFailedToStartEvent event) {
        log.error("Kafka consumer FAILED TO START (never connected to the broker): {}", event);
    }

    @EventListener
    public void onStopped(ConsumerStoppedEvent event) {
        log.warn("Kafka consumer stopped: {}", event);
    }

    @EventListener
    public void onIdle(ListenerContainerIdleEvent event) {
        log.debug("Listener container idle (no messages received recently) for {}: {}",
                event.getListenerId(), event);
    }

    @EventListener
    public void onNonResponsive(NonResponsiveConsumerEvent event) {
        log.warn("Consumer hasn't polled recently - may be stuck processing a record: {}", event);
    }
}
