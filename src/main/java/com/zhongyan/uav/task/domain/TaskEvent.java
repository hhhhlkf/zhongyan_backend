package com.zhongyan.uav.task.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record TaskEvent(
        String eventId,
        String taskId,
        TaskEventType eventType,
        TaskStatus statusBefore,
        TaskStatus statusAfter,
        Map<String, Object> payload,
        Instant createdAt) {
    /**
     * 校验事件必填字段，并规范化事件载荷。
     */
    public TaskEvent {
        requireText(eventId, "eventId");
        requireText(taskId, "taskId");
        Objects.requireNonNull(eventType, "eventType must not be null");
        payload = payload == null ? Map.of() : Map.copyOf(payload);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /**
     * 创建 Task 状态变化事件。
     */
    public static TaskEvent statusChanged(String eventId, String taskId, TaskEventType eventType,
                                          TaskStatus statusBefore, TaskStatus statusAfter, Instant now) {
        return new TaskEvent(eventId, taskId, eventType, statusBefore, statusAfter, Map.of(), now);
    }

    /**
     * 校验文本字段必须有实际内容。
     */
    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
