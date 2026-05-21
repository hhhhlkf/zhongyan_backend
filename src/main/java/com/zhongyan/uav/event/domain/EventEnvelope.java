package com.zhongyan.uav.event.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 统一事件信封。
 * <p>
 * 所有任务、资产、遥测和 Agent 事件在进入 Kafka、outbox 或实时推送前都会包装为该模型，
 * 以保证主题、消息键、载荷、头信息和发布状态有一致的审计结构。
 */
public record EventEnvelope(
        String eventId,
        String aggregateType,
        String aggregateId,
        EventType eventType,
        String topic,
        String messageKey,
        Map<String, Object> payload,
        Map<String, Object> headers,
        OutboxStatus status,
        Instant createdAt,
        Instant publishedAt,
        int retryCount,
        String lastError) {
    public EventEnvelope {
        requireText(eventId, "eventId");
        requireText(aggregateType, "aggregateType");
        requireText(aggregateId, "aggregateId");
        Objects.requireNonNull(eventType, "eventType must not be null");
        topic = defaultText(topic, eventType.topic());
        messageKey = defaultText(messageKey, aggregateId);
        payload = payload == null ? Map.of() : Map.copyOf(payload);
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        status = status == null ? OutboxStatus.PENDING : status;
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (retryCount < 0) {
            throw new IllegalArgumentException("retryCount must not be negative");
        }
    }

    public static EventEnvelope pending(String eventId, String aggregateType, String aggregateId,
                                        EventType eventType, Map<String, Object> payload,
                                        Map<String, Object> headers, Instant now) {
        return new EventEnvelope(eventId, aggregateType, aggregateId, eventType, eventType.topic(),
                aggregateId, payload, headers, OutboxStatus.PENDING, now, null, 0, null);
    }

    public EventEnvelope markPublished(Instant now) {
        return new EventEnvelope(eventId, aggregateType, aggregateId, eventType, topic, messageKey,
                payload, headers, OutboxStatus.PUBLISHED, createdAt, now, retryCount, null);
    }

    public EventEnvelope markFailed(String errorMessage) {
        return new EventEnvelope(eventId, aggregateType, aggregateId, eventType, topic, messageKey,
                payload, headers, OutboxStatus.FAILED, createdAt, publishedAt, retryCount + 1,
                defaultText(errorMessage, "event publish failed"));
    }

    public EventEnvelope retry() {
        return new EventEnvelope(eventId, aggregateType, aggregateId, eventType, topic, messageKey,
                payload, headers, OutboxStatus.PENDING, createdAt, publishedAt, retryCount, lastError);
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
