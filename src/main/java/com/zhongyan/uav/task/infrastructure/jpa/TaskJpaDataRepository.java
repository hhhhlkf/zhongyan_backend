package com.zhongyan.uav.task.infrastructure.jpa;

import com.zhongyan.uav.task.domain.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskJpaDataRepository extends JpaRepository<JpaTaskEntity, String> {
    List<JpaTaskEntity> findByMissionIdOrderByCreatedAtAsc(String missionId);

    List<JpaTaskEntity> findByStatusOrderByCreatedAtAsc(TaskStatus status);
}
