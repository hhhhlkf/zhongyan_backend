package com.zhongyan.uav.task.infrastructure.jpa;

import com.zhongyan.uav.task.domain.TaskEvent;
import com.zhongyan.uav.task.domain.TaskEventType;
import com.zhongyan.uav.task.domain.TaskStatus;
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
@Table(name = "task_event")
public class JpaTaskEventEntity {
    @Id
    @Column(name = "event_id", length = 80)
    private String eventId;

    @Column(name = "task_id", nullable = false, length = 80)
    private String taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private TaskEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_before", length = 32)
    private TaskStatus statusBefore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_after", length = 32)
    private TaskStatus statusAfter;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = Map.of();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected JpaTaskEventEntity() {
    }

    public static JpaTaskEventEntity fromDomain(TaskEvent event) {
        JpaTaskEventEntity entity = new JpaTaskEventEntity();
        entity.eventId = event.eventId();
        entity.taskId = event.taskId();
        entity.eventType = event.eventType();
        entity.statusBefore = event.statusBefore();
        entity.statusAfter = event.statusAfter();
        entity.payload = event.payload();
        entity.createdAt = event.createdAt();
        return entity;
    }

    public TaskEvent toDomain() {
        return new TaskEvent(eventId, taskId, eventType, statusBefore, statusAfter, payload, createdAt);
    }
}
