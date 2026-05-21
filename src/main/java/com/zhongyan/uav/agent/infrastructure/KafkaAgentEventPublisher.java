package com.zhongyan.uav.agent.infrastructure;

import com.zhongyan.uav.agent.port.AgentEvent;
import com.zhongyan.uav.agent.port.AgentEventPublisher;
import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.domain.EventType;
import com.zhongyan.uav.event.domain.OutboxStatus;
import com.zhongyan.uav.event.port.EventPublisher;
import com.zhongyan.uav.realtime.application.RealtimePushService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Agent 事件发布适配器。
 * <p>
 * 实现会把 {@link AgentEvent} 转换为统一 {@link EventEnvelope}，写入
 * {@code agent-events} 主题，并同步推送给当前 JVM 内的 SSE 订阅者。事件总线
 * 或实时推送不可用时不阻断主业务链路，事件仍会保留在本地发布记录中便于测试和诊断。
 */
public class KafkaAgentEventPublisher implements AgentEventPublisher {
    private final List<AgentEvent> publishedEvents = new CopyOnWriteArrayList<>();
    private final EventPublisher eventPublisher;
    private final RealtimePushService realtimePushService;

    public KafkaAgentEventPublisher() {
        this(null, null);
    }

    public KafkaAgentEventPublisher(EventPublisher eventPublisher, RealtimePushService realtimePushService) {
        this.eventPublisher = eventPublisher;
        this.realtimePushService = realtimePushService;
    }

    @Override
    public void publish(AgentEvent event) {
        publishedEvents.add(event);
        EventEnvelope envelope = toEnvelope(event);
        publishToEventBus(envelope);
        pushRealtime(envelope);
    }

    public List<AgentEvent> publishedEvents() {
        return List.copyOf(publishedEvents);
    }

    private void publishToEventBus(EventEnvelope envelope) {
        if (eventPublisher == null) {
            return;
        }
        try {
            eventPublisher.publish(envelope);
        } catch (RuntimeException ignored) {
            // Agent 对话主链路不能因为事件总线短暂不可用而失败；后续由外部监控发现 Kafka 故障。
        }
    }

    private void pushRealtime(EventEnvelope envelope) {
        if (realtimePushService == null) {
            return;
        }
        try {
            realtimePushService.push(envelope);
        } catch (RuntimeException ignored) {
            // SSE 是旁路通知能力，失败时不改变 Agent 会话和工具审计的持久化结果。
        }
    }

    private EventEnvelope toEnvelope(AgentEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("agentEventType", event.eventType());
        payload.put("agentEventId", event.eventId());
        payload.put("sessionId", event.sessionId());
        payload.put("occurredAt", event.occurredAt().toString());
        payload.put("data", event.payload());
        Map<String, Object> headers = Map.of(
                "sseEventName", event.eventType(),
                "source", "agent");
        return new EventEnvelope(event.eventId(), "AGENT_SESSION", event.sessionId(),
                EventType.AGENT_EVENT, EventType.AGENT_EVENT.topic(), event.sessionId(),
                payload, headers, OutboxStatus.PUBLISHED, event.occurredAt(),
                event.occurredAt(), 0, null);
    }
}
