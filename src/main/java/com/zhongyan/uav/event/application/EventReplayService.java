package com.zhongyan.uav.event.application;

import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.port.OutboxRepository;
import com.zhongyan.uav.realtime.application.RealtimePushService;

import java.util.List;
import java.util.Objects;

public class EventReplayService {
    private final OutboxRepository outboxRepository;
    private final RealtimePushService realtimePushService;

    public EventReplayService(OutboxRepository outboxRepository, RealtimePushService realtimePushService) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.realtimePushService = Objects.requireNonNull(realtimePushService, "realtimePushService must not be null");
    }

    public List<EventEnvelope> replayAggregate(String aggregateType, String aggregateId) {
        List<EventEnvelope> events = outboxRepository.findByAggregate(aggregateType, aggregateId);
        events.forEach(realtimePushService::push);
        return events;
    }
}
