package com.zhongyan.uav.mission.infrastructure.jpa;

import com.zhongyan.uav.mission.domain.Mission;
import com.zhongyan.uav.mission.domain.MissionRepository;
import com.zhongyan.uav.mission.domain.MissionStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "jpa")
@Transactional(readOnly = true)
public class JpaMissionRepository implements MissionRepository {
    private final MissionJpaDataRepository dataRepository;

    public JpaMissionRepository(MissionJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public Mission save(Mission mission) {
        return dataRepository.save(JpaMissionEntity.fromDomain(mission)).toDomain();
    }

    @Override
    public Optional<Mission> findById(String missionId) {
        return dataRepository.findById(missionId).map(JpaMissionEntity::toDomain);
    }

    @Override
    public List<Mission> findByStatus(MissionStatus status) {
        return dataRepository.findByStatusOrderByCreatedAtAsc(status).stream()
                .map(JpaMissionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(String missionId) {
        return dataRepository.existsById(missionId);
    }
}
