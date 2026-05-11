package com.zhongyan.uav.task.infrastructure.jpa;

import com.zhongyan.uav.task.domain.Task;
import com.zhongyan.uav.task.domain.TaskRepository;
import com.zhongyan.uav.task.domain.TaskStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "jpa")
@Transactional(readOnly = true)
public class JpaTaskRepository implements TaskRepository {
    private final TaskJpaDataRepository dataRepository;

    public JpaTaskRepository(TaskJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public Task save(Task task) {
        return dataRepository.save(JpaTaskEntity.fromDomain(task)).toDomain();
    }

    @Override
    public Optional<Task> findById(String taskId) {
        return dataRepository.findById(taskId).map(JpaTaskEntity::toDomain);
    }

    @Override
    public List<Task> findByMissionId(String missionId) {
        return dataRepository.findByMissionIdOrderByCreatedAtAsc(missionId).stream()
                .map(JpaTaskEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByStatus(TaskStatus status) {
        return dataRepository.findByStatusOrderByCreatedAtAsc(status).stream()
                .map(JpaTaskEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(String taskId) {
        return dataRepository.existsById(taskId);
    }
}
