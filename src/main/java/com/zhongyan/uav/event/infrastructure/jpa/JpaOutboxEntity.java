package com.zhongyan.uav.event.infrastructure.jpa;

import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.domain.EventType;
import com.zhongyan.uav.event.domain.OutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "event_outbox")
public class JpaOutboxEntity {
    @Id
    @Column(name = "event_id", length = 120)
    private String eventId;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 120)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 120)
    private EventType eventType;

    @Column(name = "topic", nullable = false, length = 120)
    private String topic;

    @Column(name = "message_key", nullable = false, length = 160)
    private String messageKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> headers = Map.of();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error")
    private String lastError;

    protected JpaOutboxEntity() {
    }

    public static JpaOutboxEntity fromDomain(EventEnvelope event) {
        JpaOutboxEntity entity = new JpaOutboxEntity();
        entity.eventId = event.eventId();
        entity.aggregateType = event.aggregateType();
        entity.aggregateId = event.aggregateId();
        entity.eventType = event.eventType();
        entity.topic = event.topic();
        entity.messageKey = event.messageKey();
        entity.payload = event.payload();
        entity.headers = event.headers();
        entity.status = event.status();
        entity.createdAt = event.createdAt();
        entity.publishedAt = event.publishedAt();
        entity.retryCount = event.retryCount();
        entity.lastError = event.lastError();
        return entity;
    }

    public EventEnvelope toDomain() {
        return new EventEnvelope(eventId, aggregateType, aggregateId, eventType, topic, messageKey,
                payload, headers, status, createdAt, publishedAt, retryCount, lastError);
    }
}
