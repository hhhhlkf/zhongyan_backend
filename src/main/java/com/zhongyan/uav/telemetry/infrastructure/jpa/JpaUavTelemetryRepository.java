package com.zhongyan.uav.telemetry.infrastructure.jpa;

import com.zhongyan.uav.telemetry.domain.UavTelemetry;
import com.zhongyan.uav.telemetry.domain.UavTelemetryRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "jpa")
@Transactional(readOnly = true)
public class JpaUavTelemetryRepository implements UavTelemetryRepository {
    private final UavTelemetryJpaDataRepository dataRepository;

    public JpaUavTelemetryRepository(UavTelemetryJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public UavTelemetry save(UavTelemetry telemetry) {
        return dataRepository.save(JpaUavTelemetryEntity.fromDomain(telemetry)).toDomain();
    }

    @Override
    public Optional<UavTelemetry> findLatestByUavId(String uavId) {
        return dataRepository.findFirstByUavIdOrderByRecordedAtDesc(uavId)
                .map(JpaUavTelemetryEntity::toDomain);
    }

    @Override
    public List<UavTelemetry> findByUavIdAndRecordedAtBetween(String uavId, Instant from, Instant to, int limit) {
        return dataRepository.findByUavIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                        uavId, from, to, PageRequest.of(0, Math.max(limit, 1))).stream()
                .map(JpaUavTelemetryEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UavTelemetry> findTrack(String uavId, String missionId, int limit) {
        List<JpaUavTelemetryEntity> rows = missionId == null || missionId.isBlank()
                ? dataRepository.findByUavIdOrderByRecordedAtAsc(uavId, PageRequest.of(0, Math.max(limit, 1)))
                : dataRepository.findByUavIdAndMissionIdOrderByRecordedAtAsc(
                        uavId, missionId, PageRequest.of(0, Math.max(limit, 1)));
        return rows.stream()
                .map(JpaUavTelemetryEntity::toDomain)
                .filter(telemetry -> telemetry.latitude() != null && telemetry.longitude() != null)
                .collect(Collectors.toList());
    }
}
