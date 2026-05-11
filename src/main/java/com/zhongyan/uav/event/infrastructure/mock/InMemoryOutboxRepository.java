package com.zhongyan.uav.event.infrastructure.mock;

import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.domain.EventType;
import com.zhongyan.uav.event.domain.OutboxStatus;
import com.zhongyan.uav.event.port.OutboxRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryOutboxRepository implements OutboxRepository {
    private final Map<String, EventEnvelope> events = new ConcurrentHashMap<>();

    @Override
    public EventEnvelope save(EventEnvelope event) {
        events.put(event.eventId(), event);
        return event;
    }

    @Override
    public Optional<EventEnvelope> findById(String eventId) {
        return Optional.ofNullable(events.get(eventId));
    }

    @Override
    public List<EventEnvelope> findByStatus(OutboxStatus status, int limit) {
        return events.values().stream()
                .filter(event -> event.status() == status)
                .sorted(Comparator.comparing(EventEnvelope::createdAt))
                .limit(Math.max(limit, 0))
                .collect(Collectors.toList());
    }

    @Override
    public List<EventEnvelope> findByAggregate(String aggregateType, String aggregateId) {
        return events.values().stream()
                .filter(event -> event.aggregateType().equals(aggregateType))
                .filter(event -> event.aggregateId().equals(aggregateId))
                .sorted(Comparator.comparing(EventEnvelope::createdAt))
                .collect(Collectors.toList());
    }

    @Override
    public List<EventEnvelope> findByEventType(EventType eventType, int limit) {
        return events.values().stream()
                .filter(event -> event.eventType() == eventType)
                .sorted(Comparator.comparing(EventEnvelope::createdAt))
                .limit(Math.max(limit, 0))
                .collect(Collectors.toList());
    }
}
