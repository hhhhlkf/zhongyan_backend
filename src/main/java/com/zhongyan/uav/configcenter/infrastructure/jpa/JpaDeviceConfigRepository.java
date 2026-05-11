package com.zhongyan.uav.configcenter.infrastructure.jpa;

import com.zhongyan.uav.configcenter.domain.DeviceConfig;
import com.zhongyan.uav.configcenter.domain.DeviceConfigRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "jpa")
@Transactional(readOnly = true)
public class JpaDeviceConfigRepository implements DeviceConfigRepository {
    private final DeviceConfigJpaDataRepository dataRepository;

    public JpaDeviceConfigRepository(DeviceConfigJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public DeviceConfig save(DeviceConfig config) {
        return dataRepository.save(JpaDeviceConfigEntity.fromDomain(config)).toDomain();
    }

    @Override
    public Optional<DeviceConfig> findById(String deviceId) {
        return dataRepository.findById(deviceId).map(JpaDeviceConfigEntity::toDomain);
    }

    @Override
    public List<DeviceConfig> findAll() {
        return dataRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(JpaDeviceConfigEntity::toDomain)
                .collect(Collectors.toList());
    }
}
