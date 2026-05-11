package com.zhongyan.uav.task.infrastructure.jpa;

import com.zhongyan.uav.task.domain.TaskCommand;
import com.zhongyan.uav.task.domain.TaskCommandRepository;
import com.zhongyan.uav.task.domain.TaskCommandStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "jpa")
@Transactional(readOnly = true)
public class JpaTaskCommandRepository implements TaskCommandRepository {
    private final TaskCommandJpaDataRepository dataRepository;

    public JpaTaskCommandRepository(TaskCommandJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public TaskCommand save(TaskCommand command) {
        return dataRepository.save(JpaTaskCommandEntity.fromDomain(command)).toDomain();
    }

    @Override
    public Optional<TaskCommand> findById(String commandId) {
        return dataRepository.findById(commandId).map(JpaTaskCommandEntity::toDomain);
    }

    @Override
    public Optional<TaskCommand> findByIdempotencyKey(String idempotencyKey) {
        return dataRepository.findByIdempotencyKey(idempotencyKey).map(JpaTaskCommandEntity::toDomain);
    }

    @Override
    public List<TaskCommand> findByTaskId(String taskId) {
        return dataRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(JpaTaskCommandEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskCommand> findByStatus(TaskCommandStatus status) {
        return dataRepository.findByStatusOrderByCreatedAtAsc(status).stream()
                .map(JpaTaskCommandEntity::toDomain)
                .collect(Collectors.toList());
    }
}
