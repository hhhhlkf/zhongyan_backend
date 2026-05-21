package com.zhongyan.uav.realtime.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.event.domain.EventEnvelope;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class WebSocketRealtimePushService extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final Map<String, List<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public WebSocketRealtimePushService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String topic = attribute(session, "topic");
        String key = attribute(session, "key");
        String subscriptionKey = subscriptionKey(topic, key);
        sessions.computeIfAbsent(subscriptionKey, ignored -> new CopyOnWriteArrayList<>()).add(session);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", "subscribed",
                "topic", topic,
                "key", key))));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.values().forEach(values -> values.remove(session));
    }

    public void push(EventEnvelope event) {
        send(subscriptionKey(event.topic(), event.messageKey()), event);
        send(subscriptionKey(event.topic(), "*"), event);
    }

    private void send(String subscriptionKey, EventEnvelope event) {
        List<WebSocketSession> subscribers = sessions.getOrDefault(subscriptionKey, List.of());
        for (WebSocketSession session : subscribers) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
                }
            } catch (Exception exception) {
                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private String attribute(WebSocketSession session, String name) {
        Object value = session.getAttributes().get(name);
        return value == null ? "*" : String.valueOf(value);
    }

    private String subscriptionKey(String topic, String key) {
        return topic + ":" + key;
    }
}
