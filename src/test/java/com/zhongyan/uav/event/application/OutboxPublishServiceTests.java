package com.zhongyan.uav.event.application;

import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.domain.EventType;
import com.zhongyan.uav.event.domain.OutboxStatus;
import com.zhongyan.uav.event.infrastructure.mock.InMemoryEventPublisher;
import com.zhongyan.uav.event.infrastructure.mock.InMemoryOutboxRepository;
import com.zhongyan.uav.realtime.infrastructure.SseRealtimePushService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxPublishServiceTests {
    private final Clock clock = Clock.fixed(Instant.parse("2026-04-29T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void publishesPendingOutboxEventAndMarksItPublished() {
        InMemoryOutboxRepository repository = new InMemoryOutboxRepository();
        InMemoryEventPublisher publisher = new InMemoryEventPublisher();
        OutboxPublishService service = new OutboxPublishService(repository, publisher,
                new SseRealtimePushService(), clock);

        EventEnvelope event = service.enqueue("TASK", "task-1", EventType.TASK_EVENT,
                Map.of("status", "COMPLETED"), Map.of("traceId", "trace-1"));
        List<OutboxPublishResult> results = service.publishPending(10);

        assertThat(results).singleElement().extracting(OutboxPublishResult::success).isEqualTo(true);
        assertThat(publisher.publishedEvents()).extracting(EventEnvelope::eventId)
                .containsExactly(event.eventId());
        assertThat(repository.findById(event.eventId())).get()
                .extracting(EventEnvelope::status)
                .isEqualTo(OutboxStatus.PUBLISHED);
    }

    @Test
    void replayPushesAggregateEventsWithoutChangingStatus() {
        InMemoryOutboxRepository repository = new InMemoryOutboxRepository();
        EventEnvelope event = repository.save(EventEnvelope.pending("event-1", "TASK", "task-1",
                EventType.TASK_EVENT, Map.of("status", "RUNNING"), Map.of(), clock.instant()));
        EventReplayService replayService = new EventReplayService(repository, new SseRealtimePushService());

        assertThat(replayService.replayAggregate("TASK", "task-1")).containsExactly(event);
        assertThat(repository.findById(event.eventId())).get()
                .extracting(EventEnvelope::status)
                .isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    void recordsDeadLetterAfterPublishRetryLimit() {
        InMemoryOutboxRepository repository = new InMemoryOutboxRepository();
        DeadLetterEventService deadLetterEventService = new DeadLetterEventService(repository, clock);
        OutboxPublishService service = new OutboxPublishService(repository,
                event -> {
                    throw new IllegalStateException("kafka unavailable");
                },
                new SseRealtimePushService(), deadLetterEventService, clock, 1);

        EventEnvelope event = service.enqueue("TASK", "task-2", EventType.TASK_EVENT,
                Map.of("status", "FAILED"), Map.of());

        assertThat(service.publishPending(10)).singleElement()
                .extracting(OutboxPublishResult::success)
                .isEqualTo(false);
        assertThat(repository.findById(event.eventId())).get()
                .satisfies(failed -> {
                    assertThat(failed.status()).isEqualTo(OutboxStatus.FAILED);
                    assertThat(failed.retryCount()).isEqualTo(1);
                });
        assertThat(deadLetterEventService.listDeadLetters(10)).singleElement()
                .satisfies(deadLetter -> {
                    assertThat(deadLetter.status()).isEqualTo(OutboxStatus.PENDING);
                    assertThat(deadLetter.payload()).containsEntry("sourceTopic", "task-events");
                    Object metadata = deadLetter.payload().get("metadata");
                    assertThat(metadata).isInstanceOf(Map.class);
                    assertThat(((Map<?, ?>) metadata).get("sourceEventId")).isEqualTo(event.eventId());
                });
    }

    @Test
    void recordsAndListsDeadLetterEvents() {
        InMemoryOutboxRepository repository = new InMemoryOutboxRepository();
        DeadLetterEventService deadLetterEventService = new DeadLetterEventService(repository, clock);

        EventEnvelope deadLetter = deadLetterEventService.record("task-events", "task-3", "{bad",
                "json parse failed", Map.of("stage", "deserialize"));

        assertThat(deadLetterEventService.listDeadLetters(10)).containsExactly(deadLetter);
        assertThat(repository.findById(deadLetter.eventId())).get()
                .extracting(EventEnvelope::eventType)
                .isEqualTo(EventType.DEAD_LETTER);
    }
}
