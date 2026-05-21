package com.zhongyan.uav.telemetry.infrastructure.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UavTelemetryJpaDataRepository extends JpaRepository<JpaUavTelemetryEntity, String> {
    Optional<JpaUavTelemetryEntity> findFirstByUavIdOrderByRecordedAtDesc(String uavId);

    List<JpaUavTelemetryEntity> findByUavIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            String uavId, Instant from, Instant to, Pageable pageable);

    List<JpaUavTelemetryEntity> findByUavIdOrderByRecordedAtAsc(String uavId, Pageable pageable);

    List<JpaUavTelemetryEntity> findByUavIdAndMissionIdOrderByRecordedAtAsc(
            String uavId, String missionId, Pageable pageable);
}
