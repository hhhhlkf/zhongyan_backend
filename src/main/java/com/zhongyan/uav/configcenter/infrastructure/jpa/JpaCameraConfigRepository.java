package com.zhongyan.uav.configcenter.infrastructure.jpa;

import com.zhongyan.uav.configcenter.domain.CameraConfigRepository;
import com.zhongyan.uav.configcenter.domain.CameraConfigVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "jpa")
@Transactional(readOnly = true)
public class JpaCameraConfigRepository implements CameraConfigRepository {
    private final CameraConfigJpaDataRepository dataRepository;

    public JpaCameraConfigRepository(CameraConfigJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public CameraConfigVersion save(CameraConfigVersion config) {
        return dataRepository.save(JpaCameraConfigEntity.fromDomain(config)).toDomain();
    }

    @Override
    public Optional<CameraConfigVersion> findById(String cameraConfigId, int version) {
        return dataRepository.findByCameraConfigIdAndVersion(cameraConfigId, version)
                .map(JpaCameraConfigEntity::toDomain);
    }

    @Override
    public Optional<CameraConfigVersion> findLatestByConfigId(String cameraConfigId) {
        return dataRepository.findTopByCameraConfigIdOrderByVersionDesc(cameraConfigId)
                .map(JpaCameraConfigEntity::toDomain);
    }

    @Override
    public List<CameraConfigVersion> findByConfigId(String cameraConfigId) {
        return dataRepository.findByCameraConfigIdOrderByVersionAsc(cameraConfigId).stream()
                .map(JpaCameraConfigEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CameraConfigVersion> findAllLatest() {
        return dataRepository.findAllLatest().stream()
                .map(JpaCameraConfigEntity::toDomain)
                .collect(Collectors.toList());
    }
}
