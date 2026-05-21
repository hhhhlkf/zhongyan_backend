package com.zhongyan.uav.event.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongyan.uav.event.application.DeadLetterEventService;
import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.domain.EventType;
import com.zhongyan.uav.event.infrastructure.mock.InMemoryOutboxRepository;
import com.zhongyan.uav.realtime.application.RealtimePushService;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance tests for Kafka event consumption without requiring a real broker.
 */
class KafkaEventConsumerTests {
    private static final Instant NOW = Instant.parse("2026-05-19T08:00:00Z");

    @Test
    void pushesConsumedEventToRealtimeOnlyOnceByEventId() {
        CapturingRealtimePushService realtimePushService = new CapturingRealtimePushService();
        DeadLetterEventService deadLetterEventService = new DeadLetterEventService(new InMemoryOutboxRepository());
        KafkaEventConsumer consumer = new KafkaEventConsumer(new ObjectMapper(),
                realtimePushService, deadLetterEventService);
        EventEnvelope event = agentEvent("agent-event-1", "session-1");

        consumer.onEvent(event);
        consumer.onEvent(event);

        assertThat(realtimePushService.events()).containsExactly(event);
        assertThat(deadLetterEventService.listDeadLetters(10)).isEmpty();
    }

    @Test
    void recordsDeadLetterWhenRealtimePushFails() {
        InMemoryOutboxRepository outboxRepository = new InMemoryOutboxRepository();
        DeadLetterEventService deadLetterEventService = new DeadLetterEventService(outboxRepository);
        KafkaEventConsumer consumer = new KafkaEventConsumer(new ObjectMapper(),
                new FailingRealtimePushService(), deadLetterEventService);
        EventEnvelope event = agentEvent("agent-event-2", "session-2");

        consumer.onEvent(event);

        assertThat(deadLetterEventService.listDeadLetters(10)).singleElement()
                .satisfies(deadLetter -> {
                    assertThat(deadLetter.payload()).containsEntry("sourceTopic", "agent-events");
                    assertThat(deadLetter.payload()).containsEntry("messageKey", "session-2");
                    Object metadata = deadLetter.payload().get("metadata");
                    assertThat(metadata).isInstanceOf(Map.class);
                    assertThat(((Map<?, ?>) metadata).get("stage")).isEqualTo("realtime-push");
                    assertThat(((Map<?, ?>) metadata).get("sourceEventId")).isEqualTo(event.eventId());
                });
    }

    private static EventEnvelope agentEvent(String eventId, String sessionId) {
        return EventEnvelope.pending(eventId, "AGENT_SESSION", sessionId, EventType.AGENT_EVENT,
                Map.of("agentEventType", "ANSWER_STREAMED"), Map.of("sseEventName", "ANSWER_STREAMED"), NOW);
    }

    /**
     * Test double that records events sent to realtime push.
     */
    private static final class CapturingRealtimePushService implements RealtimePushService {
        private final List<EventEnvelope> events = new ArrayList<>();

        @Override
        public SseEmitter subscribe(String topic, String key) {
            return new SseEmitter();
        }

        @Override
        public void push(EventEnvelope event) {
            events.add(event);
        }

        private List<EventEnvelope> events() {
            return events;
        }
    }

    /**
     * Test double that simulates a broken realtime push backend.
     */
    private static final class FailingRealtimePushService implements RealtimePushService {
        @Override
        public SseEmitter subscribe(String topic, String key) {
            return new SseEmitter();
        }

        @Override
        public void push(EventEnvelope event) {
            throw new IllegalStateException("sse unavailable");
        }
    }
}
