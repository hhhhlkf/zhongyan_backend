package com.zhongyan.uav.event.port;

import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.domain.EventType;
import com.zhongyan.uav.event.domain.OutboxStatus;

import java.util.List;
import java.util.Optional;

public interface OutboxRepository {
    EventEnvelope save(EventEnvelope event);

    Optional<EventEnvelope> findById(String eventId);

    List<EventEnvelope> findByStatus(OutboxStatus status, int limit);

    List<EventEnvelope> findByAggregate(String aggregateType, String aggregateId);

    List<EventEnvelope> findByEventType(EventType eventType, int limit);
}
