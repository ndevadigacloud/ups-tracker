package com.ups.shipment.sse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class ShipmentSseHub {

    private static final Logger log = LoggerFactory.getLogger(ShipmentSseHub.class);

    private static final long TIMEOUT_MS = 15 * 60 * 1000L;

    private final Map<String, List<SseEmitter>> emittersByShipmentId = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String shipmentId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        List<SseEmitter> emitters = emittersByShipmentId.computeIfAbsent(shipmentId, id -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);
        log.info("SSE subscriber added for shipment {} (active subscribers: {})", shipmentId, emitters.size());

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("SSE subscriber completed for shipment {}", shipmentId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.debug("SSE subscriber timed out for shipment {}", shipmentId);
        });
        emitter.onError(e -> {
            emitters.remove(emitter);
            log.debug("SSE subscriber errored for shipment {}: {}", shipmentId, e.getMessage());
        });

        return emitter;
    }

    public void publish(String shipmentId, String eventName, Object payload) {
        List<SseEmitter> emitters = emittersByShipmentId.get(shipmentId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No SSE subscribers for shipment {}, dropping '{}' event", shipmentId, eventName);
            return;
        }
        log.debug("Publishing SSE '{}' event for shipment {} to {} subscriber(s)", eventName, shipmentId, emitters.size());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException e) {
                log.debug("Failed to send SSE event to a subscriber for shipment {}, removing it: {}", shipmentId, e.getMessage());
                emitters.remove(emitter);
            }
        }
    }
}
