package com.zhongyan.uav.event.application;

import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.domain.EventType;
import com.zhongyan.uav.event.port.OutboxRepository;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class DeadLetterEventService {
    private final OutboxRepository outboxRepository;
    private final Clock clock;

    public DeadLetterEventService(OutboxRepository outboxRepository) {
        this(outboxRepository, Clock.systemUTC());
    }

    public DeadLetterEventService(OutboxRepository outboxRepository, Clock clock) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public EventEnvelope record(String sourceTopic, String messageKey, String rawMessage,
                                String errorMessage, Map<String, Object> metadata) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceTopic", defaultText(sourceTopic, "unknown"));
        payload.put("messageKey", defaultText(messageKey, "unknown"));
        payload.put("rawMessage", defaultText(rawMessage, ""));
        payload.put("errorMessage", defaultText(errorMessage, "dead letter"));
        payload.put("metadata", metadata == null ? Map.of() : Map.copyOf(metadata));
        EventEnvelope event = EventEnvelope.pending("event-" + UUID.randomUUID(), "DEAD_LETTER",
                defaultText(messageKey, "unknown"), EventType.DEAD_LETTER, payload, Map.of(), clock.instant());
        return outboxRepository.save(event);
    }

    public EventEnvelope recordPublishFailure(EventEnvelope sourceEvent, String errorMessage) {
        return record(sourceEvent.topic(), sourceEvent.messageKey(), sourceEvent.eventId(), errorMessage,
                Map.of("sourceEventId", sourceEvent.eventId(),
                        "sourceEventType", sourceEvent.eventType().name(),
                        "retryCount", sourceEvent.retryCount()));
    }

    public List<EventEnvelope> listDeadLetters(int limit) {
        return outboxRepository.findByEventType(EventType.DEAD_LETTER, limit);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
