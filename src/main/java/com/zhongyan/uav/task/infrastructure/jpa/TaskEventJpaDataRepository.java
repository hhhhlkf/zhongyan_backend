package com.zhongyan.uav.task.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskEventJpaDataRepository extends JpaRepository<JpaTaskEventEntity, String> {
    List<JpaTaskEventEntity> findByTaskIdOrderByCreatedAtAsc(String taskId);
}
