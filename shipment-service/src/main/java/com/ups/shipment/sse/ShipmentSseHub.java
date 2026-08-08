package com.ups.shipment.sse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class ShipmentSseHub {

    private static final long TIMEOUT_MS = 15 * 60 * 1000L;

    private final Map<String, List<SseEmitter>> emittersByShipmentId = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String shipmentId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        List<SseEmitter> emitters = emittersByShipmentId.computeIfAbsent(shipmentId, id -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        return emitter;
    }

    public void publish(String shipmentId, String eventName, Object payload) {
        List<SseEmitter> emitters = emittersByShipmentId.get(shipmentId);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}
