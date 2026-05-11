package com.zhongyan.uav.task.api.response;

import com.zhongyan.uav.task.domain.TaskEvent;
import com.zhongyan.uav.task.domain.TaskEventType;
import com.zhongyan.uav.task.domain.TaskStatus;

import java.time.Instant;
import java.util.Map;

public record TaskEventView(
        String eventId,
        String taskId,
        TaskEventType eventType,
        TaskStatus statusBefore,
        TaskStatus statusAfter,
        Map<String, Object> payload,
        Instant createdAt) {
    /**
     * 将 TaskEvent 领域对象转换为 API 响应视图。
     */
    public static TaskEventView from(TaskEvent event) {
        return new TaskEventView(event.eventId(), event.taskId(), event.eventType(),
                event.statusBefore(), event.statusAfter(), event.payload(), event.createdAt());
    }
}
