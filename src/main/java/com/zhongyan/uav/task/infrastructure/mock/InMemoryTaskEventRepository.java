package com.zhongyan.uav.task.infrastructure.mock;

import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.domain.EventType;
import com.zhongyan.uav.event.port.OutboxRepository;
import com.zhongyan.uav.task.domain.TaskEvent;
import com.zhongyan.uav.task.domain.TaskEventRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryTaskEventRepository implements TaskEventRepository {
    private final Map<String, TaskEvent> events = new ConcurrentHashMap<>();
    private final OutboxRepository outboxRepository;

    public InMemoryTaskEventRepository() {
        this(null);
    }

    public InMemoryTaskEventRepository(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Override
    public TaskEvent save(TaskEvent event) {
        events.put(event.eventId(), event);
        saveOutbox(event);
        return event;
    }

    @Override
    public Optional<TaskEvent> findById(String eventId) {
        return Optional.ofNullable(events.get(eventId));
    }

    @Override
    public List<TaskEvent> findByTaskId(String taskId) {
        return events.values().stream()
                .filter(event -> event.taskId().equals(taskId))
                .sorted(Comparator.comparing(TaskEvent::createdAt))
                .collect(Collectors.toList());
    }

    private void saveOutbox(TaskEvent event) {
        if (outboxRepository == null) {
            return;
        }
        outboxRepository.save(EventEnvelope.pending("outbox-" + event.eventId(), "TASK",
                event.taskId(), EventType.TASK_EVENT, payload(event), Map.of(), event.createdAt()));
    }

    private Map<String, Object> payload(TaskEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>(event.payload());
        payload.put("taskId", event.taskId());
        payload.put("eventType", event.eventType().name());
        if (event.statusBefore() != null) {
            payload.put("statusBefore", event.statusBefore().name());
        }
        if (event.statusAfter() != null) {
            payload.put("statusAfter", event.statusAfter().name());
        }
        return payload;
    }
}
