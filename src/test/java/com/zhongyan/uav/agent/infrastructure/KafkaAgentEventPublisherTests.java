package com.zhongyan.uav.agent.infrastructure;

import com.zhongyan.uav.agent.port.AgentEvent;
import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.domain.EventType;
import com.zhongyan.uav.event.port.EventPublisher;
import com.zhongyan.uav.realtime.application.RealtimePushService;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Agent 事件发布器会同时写入统一事件总线并通知实时推送通道。
 */
class KafkaAgentEventPublisherTests {
    @Test
    void publishesAgentEventToEventBusAndRealtimeChannel() {
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        CapturingRealtimePushService realtimePushService = new CapturingRealtimePushService();
        KafkaAgentEventPublisher publisher = new KafkaAgentEventPublisher(eventPublisher, realtimePushService);
        AgentEvent event = new AgentEvent("agent-event-1", "session-1",
                "USER_MESSAGE_RECEIVED", Map.of("messageId", "message-1"),
                Instant.parse("2026-05-19T08:00:00Z"));

        publisher.publish(event);

        assertThat(publisher.publishedEvents()).containsExactly(event);
        assertThat(eventPublisher.events()).hasSize(1);
        EventEnvelope envelope = eventPublisher.events().get(0);
        assertThat(envelope.eventId()).isEqualTo("agent-event-1");
        assertThat(envelope.eventType()).isEqualTo(EventType.AGENT_EVENT);
        assertThat(envelope.topic()).isEqualTo("agent-events");
        assertThat(envelope.messageKey()).isEqualTo("session-1");
        assertThat(envelope.headers()).containsEntry("sseEventName", "USER_MESSAGE_RECEIVED");
        assertThat(envelope.payload()).containsEntry("agentEventType", "USER_MESSAGE_RECEIVED");
        assertThat(realtimePushService.events()).containsExactly(envelope);
    }

    @Test
    void realtimePushStillHappensWhenEventBusIsUnavailable() {
        CapturingRealtimePushService realtimePushService = new CapturingRealtimePushService();
        KafkaAgentEventPublisher publisher = new KafkaAgentEventPublisher(event -> {
            throw new IllegalStateException("kafka down");
        }, realtimePushService);
        AgentEvent event = new AgentEvent("agent-event-2", "session-2",
                "ANSWER_STREAMED", Map.of("finalChunk", true),
                Instant.parse("2026-05-19T08:00:01Z"));

        publisher.publish(event);

        assertThat(publisher.publishedEvents()).containsExactly(event);
        assertThat(realtimePushService.events()).hasSize(1);
        assertThat(realtimePushService.events().get(0).headers())
                .containsEntry("sseEventName", "ANSWER_STREAMED");
    }

    /**
     * 测试用事件发布器，只记录发布到事件总线的信封。
     */
    private static final class CapturingEventPublisher implements EventPublisher {
        private final List<EventEnvelope> events = new ArrayList<>();

        @Override
        public void publish(EventEnvelope event) {
            events.add(event);
        }

        private List<EventEnvelope> events() {
            return events;
        }
    }

    /**
     * 测试用实时推送服务，只记录被推送到 SSE 通道的事件。
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
}
