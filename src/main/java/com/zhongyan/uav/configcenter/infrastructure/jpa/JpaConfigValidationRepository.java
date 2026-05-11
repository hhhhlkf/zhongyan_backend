package com.zhongyan.uav.configcenter.infrastructure.jpa;

import com.zhongyan.uav.configcenter.domain.ConfigValidation;
import com.zhongyan.uav.configcenter.domain.ConfigValidationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "bms.repository", name = "mode", havingValue = "jpa")
@Transactional(readOnly = true)
public class JpaConfigValidationRepository implements ConfigValidationRepository {
    private final ConfigValidationJpaDataRepository dataRepository;

    public JpaConfigValidationRepository(ConfigValidationJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    @Transactional
    public ConfigValidation save(ConfigValidation validation) {
        return dataRepository.save(JpaConfigValidationEntity.fromDomain(validation)).toDomain();
    }

    @Override
    public Optional<ConfigValidation> findById(String validationId) {
        return dataRepository.findById(validationId).map(JpaConfigValidationEntity::toDomain);
    }

    @Override
    public List<ConfigValidation> findByConfig(String configType, String configId) {
        return dataRepository.findByConfigTypeAndConfigIdOrderByCreatedAtAsc(configType, configId).stream()
                .map(JpaConfigValidationEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConfigValidation> findAll() {
        return dataRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(JpaConfigValidationEntity::toDomain)
                .collect(Collectors.toList());
    }
}
