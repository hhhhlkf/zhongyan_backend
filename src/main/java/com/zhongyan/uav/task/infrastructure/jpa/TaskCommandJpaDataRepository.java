package com.zhongyan.uav.task.infrastructure.jpa;

import com.zhongyan.uav.task.domain.TaskCommandStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskCommandJpaDataRepository extends JpaRepository<JpaTaskCommandEntity, String> {
    Optional<JpaTaskCommandEntity> findByIdempotencyKey(String idempotencyKey);

    List<JpaTaskCommandEntity> findByTaskIdOrderByCreatedAtAsc(String taskId);

    List<JpaTaskCommandEntity> findByStatusOrderByCreatedAtAsc(TaskCommandStatus status);
}
