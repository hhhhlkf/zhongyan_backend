package com.zhongyan.uav.event.application;

import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.domain.EventType;
import com.zhongyan.uav.event.domain.OutboxStatus;
import com.zhongyan.uav.event.port.EventPublisher;
import com.zhongyan.uav.event.port.OutboxRepository;
import com.zhongyan.uav.realtime.application.RealtimePushService;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class OutboxPublishService {
    private final OutboxRepository outboxRepository;
    private final EventPublisher eventPublisher;
    private final RealtimePushService realtimePushService;
    private final DeadLetterEventService deadLetterEventService;
    private final Clock clock;
    private final int maxRetryCount;

    public OutboxPublishService(OutboxRepository outboxRepository,
                                EventPublisher eventPublisher,
                                RealtimePushService realtimePushService) {
        this(outboxRepository, eventPublisher, realtimePushService,
                new DeadLetterEventService(outboxRepository), Clock.systemUTC(), 3);
    }

    public OutboxPublishService(OutboxRepository outboxRepository,
                                EventPublisher eventPublisher,
                                RealtimePushService realtimePushService,
                                Clock clock) {
        this(outboxRepository, eventPublisher, realtimePushService,
                new DeadLetterEventService(outboxRepository), clock, 3);
    }

    public OutboxPublishService(OutboxRepository outboxRepository,
                                EventPublisher eventPublisher,
                                RealtimePushService realtimePushService,
                                DeadLetterEventService deadLetterEventService,
                                Clock clock,
                                int maxRetryCount) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.realtimePushService = Objects.requireNonNull(realtimePushService, "realtimePushService must not be null");
        this.deadLetterEventService = Objects.requireNonNull(deadLetterEventService, "deadLetterEventService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.maxRetryCount = Math.max(1, maxRetryCount);
    }

    public EventEnvelope enqueue(String aggregateType, String aggregateId, EventType eventType,
                                 Map<String, Object> payload, Map<String, Object> headers) {
        EventEnvelope event = EventEnvelope.pending("event-" + UUID.randomUUID(), aggregateType,
                aggregateId, eventType, payload, headers, clock.instant());
        return outboxRepository.save(event);
    }

    public List<OutboxPublishResult> publishPending(int limit) {
        return outboxRepository.findByStatus(OutboxStatus.PENDING, limit).stream()
                .map(this::publishOne)
                .collect(Collectors.toList());
    }

    public List<OutboxPublishResult> retryFailed(int limit) {
        return outboxRepository.findByStatus(OutboxStatus.FAILED, limit).stream()
                .map(EventEnvelope::retry)
                .map(outboxRepository::save)
                .map(this::publishOne)
                .collect(Collectors.toList());
    }

    private OutboxPublishResult publishOne(EventEnvelope event) {
        try {
            eventPublisher.publish(event);
            realtimePushService.push(event);
            outboxRepository.save(event.markPublished(clock.instant()));
            return new OutboxPublishResult(event.eventId(), true, "published");
        } catch (RuntimeException ex) {
            EventEnvelope failedEvent = outboxRepository.save(event.markFailed(ex.getMessage()));
            if (failedEvent.retryCount() >= maxRetryCount && failedEvent.eventType() != EventType.DEAD_LETTER) {
                deadLetterEventService.recordPublishFailure(failedEvent, ex.getMessage());
            }
            return new OutboxPublishResult(event.eventId(), false, ex.getMessage());
        }
    }
}
