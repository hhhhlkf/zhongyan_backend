package com.zhongyan.uav.mission.infrastructure.jpa;

import com.zhongyan.uav.mission.domain.MissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionJpaDataRepository extends JpaRepository<JpaMissionEntity, String> {
    List<JpaMissionEntity> findByStatusOrderByCreatedAtAsc(MissionStatus status);
}
