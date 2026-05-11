package com.zhongyan.uav.task.infrastructure.jpa;

import com.zhongyan.uav.task.domain.TaskAttempt;
import com.zhongyan.uav.task.domain.TaskAttemptRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "jpa")
@Transactional(readOnly = true)
public class JpaTaskAttemptRepository implements TaskAttemptRepository {
    private final TaskAttemptJpaDataRepository dataRepository;

    public JpaTaskAttemptRepository(TaskAttemptJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public TaskAttempt save(TaskAttempt attempt) {
        return dataRepository.save(JpaTaskAttemptEntity.fromDomain(attempt)).toDomain();
    }

    @Override
    public Optional<TaskAttempt> findById(String attemptId) {
        return dataRepository.findById(attemptId).map(JpaTaskAttemptEntity::toDomain);
    }

    @Override
    public List<TaskAttempt> findByTaskId(String taskId) {
        return dataRepository.findByTaskIdOrderByAttemptNoAsc(taskId).stream()
                .map(JpaTaskAttemptEntity::toDomain)
                .collect(Collectors.toList());
    }
}
