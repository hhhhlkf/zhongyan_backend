package com.zhongyan.uav.task.infrastructure.jpa;

import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.domain.EventType;
import com.zhongyan.uav.event.port.OutboxRepository;
import com.zhongyan.uav.task.domain.TaskEvent;
import com.zhongyan.uav.task.domain.TaskEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "jpa")
@Transactional(readOnly = true)
public class JpaTaskEventRepository implements TaskEventRepository {
    private final TaskEventJpaDataRepository dataRepository;
    private final OutboxRepository outboxRepository;

    public JpaTaskEventRepository(TaskEventJpaDataRepository dataRepository,
                                  OutboxRepository outboxRepository) {
        this.dataRepository = dataRepository;
        this.outboxRepository = outboxRepository;
    }

    @Override
    @Transactional
    public TaskEvent save(TaskEvent event) {
        TaskEvent savedEvent = dataRepository.save(JpaTaskEventEntity.fromDomain(event)).toDomain();
        outboxRepository.save(EventEnvelope.pending("outbox-" + savedEvent.eventId(), "TASK",
                savedEvent.taskId(), EventType.TASK_EVENT, payload(savedEvent), Map.of(),
                savedEvent.createdAt()));
        return savedEvent;
    }

    @Override
    public Optional<TaskEvent> findById(String eventId) {
        return dataRepository.findById(eventId).map(JpaTaskEventEntity::toDomain);
    }

    @Override
    public List<TaskEvent> findByTaskId(String taskId) {
        return dataRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(JpaTaskEventEntity::toDomain)
                .collect(Collectors.toList());
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
