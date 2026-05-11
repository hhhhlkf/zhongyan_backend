package com.zhongyan.uav.event.domain;

import java.time.Instant;
import java.util.Map;

public interface DomainEvent {
    String eventId();

    String aggregateType();

    String aggregateId();

    EventType eventType();

    Map<String, Object> payload();

    Instant occurredAt();
}
