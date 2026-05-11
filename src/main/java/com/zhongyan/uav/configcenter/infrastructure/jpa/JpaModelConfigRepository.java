package com.zhongyan.uav.configcenter.infrastructure.jpa;

import com.zhongyan.uav.configcenter.domain.ModelConfigRepository;
import com.zhongyan.uav.configcenter.domain.ModelConfigVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "jpa")
@Transactional(readOnly = true)
public class JpaModelConfigRepository implements ModelConfigRepository {
    private final ModelConfigJpaDataRepository dataRepository;

    public JpaModelConfigRepository(ModelConfigJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public ModelConfigVersion save(ModelConfigVersion config) {
        return dataRepository.save(JpaModelConfigEntity.fromDomain(config)).toDomain();
    }

    @Override
    public Optional<ModelConfigVersion> findById(String modelConfigId, int version) {
        return dataRepository.findByModelConfigIdAndVersion(modelConfigId, version)
                .map(JpaModelConfigEntity::toDomain);
    }

    @Override
    public Optional<ModelConfigVersion> findLatestByConfigId(String modelConfigId) {
        return dataRepository.findTopByModelConfigIdOrderByVersionDesc(modelConfigId)
                .map(JpaModelConfigEntity::toDomain);
    }

    @Override
    public List<ModelConfigVersion> findByConfigId(String modelConfigId) {
        return dataRepository.findByModelConfigIdOrderByVersionAsc(modelConfigId).stream()
                .map(JpaModelConfigEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ModelConfigVersion> findAllLatest() {
        return dataRepository.findAllLatest().stream()
                .map(JpaModelConfigEntity::toDomain)
                .collect(Collectors.toList());
    }
}
