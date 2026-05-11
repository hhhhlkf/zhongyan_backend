package com.zhongyan.uav.task.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskAttemptJpaDataRepository extends JpaRepository<JpaTaskAttemptEntity, String> {
    List<JpaTaskAttemptEntity> findByTaskIdOrderByAttemptNoAsc(String taskId);
}
