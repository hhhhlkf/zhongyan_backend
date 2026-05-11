package com.zhongyan.uav.realtime.infrastructure;

import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.realtime.application.RealtimePushService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SseRealtimePushService implements RealtimePushService {
    private static final long DEFAULT_TIMEOUT_MS = 30 * 60 * 1000L;
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(String topic, String key) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
        String subscriptionKey = subscriptionKey(topic, key);
        emitters.computeIfAbsent(subscriptionKey, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(subscriptionKey, emitter));
        emitter.onTimeout(() -> remove(subscriptionKey, emitter));
        try {
            emitter.send(SseEmitter.event().name("subscribed").data(Map.of("topic", topic, "key", key)));
        } catch (IOException ex) {
            remove(subscriptionKey, emitter);
        }
        return emitter;
    }

    @Override
    public void push(EventEnvelope event) {
        send(subscriptionKey(event.topic(), event.messageKey()), event);
        send(subscriptionKey(event.topic(), "*"), event);
    }

    private void send(String subscriptionKey, EventEnvelope event) {
        List<SseEmitter> subscribers = emitters.getOrDefault(subscriptionKey, List.of());
        for (SseEmitter emitter : subscribers) {
            try {
                emitter.send(SseEmitter.event()
                        .id(event.eventId())
                        .name(event.eventType().name())
                        .data(event));
            } catch (IOException ex) {
                remove(subscriptionKey, emitter);
            }
        }
    }

    private void remove(String subscriptionKey, SseEmitter emitter) {
        List<SseEmitter> subscribers = emitters.get(subscriptionKey);
        if (subscribers != null) {
            subscribers.remove(emitter);
        }
    }

    private String subscriptionKey(String topic, String key) {
        return topic + ":" + key;
    }
}
