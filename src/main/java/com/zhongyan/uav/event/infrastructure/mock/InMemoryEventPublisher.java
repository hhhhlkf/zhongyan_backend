package com.zhongyan.uav.event.infrastructure.mock;

import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.port.EventPublisher;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryEventPublisher implements EventPublisher {
    private final List<EventEnvelope> publishedEvents = new CopyOnWriteArrayList<>();

    @Override
    public void publish(EventEnvelope event) {
        publishedEvents.add(event);
    }

    public List<EventEnvelope> publishedEvents() {
        return List.copyOf(publishedEvents);
    }
}
